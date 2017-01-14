package runtime

/*
	1. Background information

		This file contains the implementation of a concurrent extension for Go's map type.

		This is a novel lock-based implementation of a hash table. Similar to Go's default
		builtin hash table (see runtime/hashmap.go), data is arranged into an array of
		buckets, wherein each bucket contains up to 8 key/value pairs. As well, we use the
		most significant byte (8-bits) of the hashto distinguish each entry in a bucket.
		If the hash is EMPTY, it signifies that the entry is valid. Further in this
		documentation, a pair of hash/key/value will be referred to as 'data slots',
		and should not be confused with pointer-sized word-aligned memory.

	2. Types

		Unlike the default builtin hash table, we have three distinct types, with their own
		unique specific purposes.

		A. bucketHdr

			The descriptor for a bucket, of which can safely be casted to and from the respective
			type it describes. For example, this can be casted to bucketData or bucketArray, and
			bucketData or bucketArray can be casted to a bucketHdr respectively. This plays a
			crucial role in that it allows us to have a collection of intermediate types
			which actually describe what it's body holds. It's significance will be outlined
			further below. Should be emphasized: both bucketData and bucketArray contain the
			same fields as this type, as they are castable.

			It holds a Test-and-Test-and-Set spinlock which not only supplies mutual exclusion,
			but as well describes the 'state' and 'type' of the bucket. As well, it contains
			a context-based counter (explained below) as well as functions as linked list where
			it it holds a reference to it's parent and it's position inside of it's parent
			(of which it's significance is explained later).

		B. bucketData

			The actual data itself: contains 8 data slots of which store data for the user.
			This type of bucket REQUIRES that the spinlock be obtained before attempting to
			access it's data slots. The values it's spinlock will ALWAYS contain are listed
			below...

			It's 'count' is used to determine how many data slots are filled.

			Spinlock Values: LOCKED | UNLOCKED

		C. bucketArray

			An array of buckets; it is used to maintain the number of buckets at this depth.
			Beginning at the root (depth = 0), the number of buckets stored by any given depth N,
			is (DEFAULT_BUCKETS * (N+1)). Hence, if DEFAULT_BUCKETS = 32, at the root (depth = 0),
			it will have 32 buckets, at depth = 1, we have 64 buckets, and so on and so forth.

			It's 'count' is used to determine how many buckets are allocated (and hence hold
			data). For reference: A bucket is originally 'nil' to signify that it holds no data.

			Spinlock Values: ARRAY


	3. Spinlock

		The Test-and-Test-and-Set Spinlock is used to not only provide mutual exclusion, but
		as well to act as a kind of descriptor for the bucket that it belongs to. If the higher
		bits ~(0x2) are set, it signifies that it is a locked bucketData (as only bucketData require
		a lock). As well, if any of the higher bits are set, the lower order bits (0x2) MUST be 0.

		If the lower order bits are set, then they can mean one of 3 things...

		00:
			Unlocked bucketData
		01:
			bucketArray
		10:
			INVALID

		If a spinlock's state describes the bucket as 'INVALID', then it signifies that it
		has changed and the any current waiting Goroutines must 'refresh' their bucket.
		Refreshing is done by doing using the bucket's backlink to it's parent and reloading
		updating it's current reference with what is there currently. This only occurs during
		resizing and deletion, both described further on.

		Note as well, a bucketArray, does not need to be acquired, and so any nested bucketArray
		can be traversed by any number of Goroutines; chceking is as cheap as an atomic load.

	4. Resizing

		The map takes a more unique approach on the issue of growth. Instead of the conventional
		means of locking all buckets within a bucketArray in a top down approach to rehash every
		element, instead we merely create a new bucketArray, rehash the elements inside of the
		full bucketData into that new bucketArray, and then update the parent's reference
		to point to the new one. We also set the INVALID bit for the spinlock notifying that
		all waiters must refresh the bucket they are working on. As well, this new
		bucketArray is twice the size of the previous, and uses a different unique seed to
		prevent any excess collision and possible contention for elements that hash to the same
		element.

		In addition, not only do you have extremely low collision of elements (given a
		depth of N, two keys can only collide to the same bucket if they happen
		to hash to the same bucket N times with a different seed and modulo), they also
		provide extremely low contention (as keys are far less likely to hash to the same
		bucket, as described above).

	5. Deletion and 'Shrinking'

		While the map does not necessarily shrink, it does provide a means to keep space
		usage down. bucketArray are never deleted, but bucketData can be. Once a bucketData
		is empty, it will be also be removed. In an experiment, filling up the map with
		10 million elements (8-byte key/value pairs) took up about 2.1GBs of memory, but
		deleting all 10 Million shrank to around 500MBs. While, again, it is not a means
		to shrink, it is rather adequate.

	6. Iteration

		Iteration is an extremely important feature of a data structure, of which not all
		lock-free algorithms provide, and most lock-based algorithms fail to do justice for.
		Iteration has one 'invariant' which may be a bit inconvenient, but at the same time
		provides high-scalability and allows concurrent (and parallel) mutations to occur
		while doing so. While iterating, you are in a mode referred to as 'interlocked access',
		which will be described in detail later. In brief: You may only hold the lock on
		one bucket at any given time (the bucket you are iterating over), and so as it intuitively
		follows...you can have as many concurrent accesses as you have buckets.

		The biggest issue with iteration is the problem of lock convoying: In brief, given
		an iterator A which takes 100ms, and an iterator B which takes 10ms, and an
		iterator C which takes 1ms, and given N buckets with M depth:

			If A holds the lock on a bucket that B wants, then B must want for A even though
			it would originally finish within 10ms (only a 1/10th the time of A), and if C
			is also waiting on A, it would take 100x as long as it would to finish iterating.
			As well, this problem gets amplified in that once A finishes, it will acquire the lock
			on the next bucket, and this issue repeats. Hence all iterators slow to the pace
			of the slowest iterator. This can cause B and C to take over 100ms to finish iterating,
			when they could have finished already.

		Now, one way to reduce the effects of convoying is to randomize our start position when we
		traverse down to the next depth. Hence, lets say that the convoying from A, B, and C occurred,
		we would be bounded in how bad convoying is in that A, B, and C are bound to go different ways and
		less likely to convoy up again.

		As well, we truly eliminate any and all convoying by keeping track of all locked buckets
		as 'busy' and poll on all of them later. This way, even if A is processing a bucket that B
		and C want, they will skip that until the end; hopefully after it finishes, it will already
		have been released. Even if it is not, when we poll on the skipped over buckets, we're
		always doing work. In experiments ran, we found that even under high contention,
		iteration always seemed to scale very well in that any locked buckets are skipped over,
		hence providing scalability.

	7. Interlocked Access

		When we say 'interlocked' we mean that the bucket is currently locked for longer than
		a single operation. Normal operations, such as insert, lookup, and removal are merely
		atomic operations. However, iteration and user-specified 'sync.Interlocked' request
		to a key is referred to as 'interlocked'. In this mode, the user may only access that
		key, and any attempts to access other keys will result in a panic. While the user has
		interlocked access to an element, they are the only ones with access to it (this also
		applies to iteration, as only you have access to that bucket). This invariant is very
		necessary to prevent deadlock, as we may only remain deadlock free while we may only
		hold one lock at any given time.

		This invariant gives way to an extremely crucial optimization, wherein the current
		locked bucket is cached and can be obtained instantly, which also GREATLY contributes to
		it's scalability.

	8. The 'live pointer' problem

		The function 'mapaccess' returns a live pointer to the element itself, and is normally
		protected by a mutex, and hence the time wherein it copies into the user requested storage
		is normally safe. However, with a concurrent map, this becomes an issue wherein once we release
		the current lock, another mutator could come in and modify the returned value before it
		finishes it's copy, resulting in undefined behavior. This means that the lock must not
		be released until after it's copy finished... however, this is not possible from the perspective
		from the runtime itself. Hence, a call to 'maprelease' is generated after it's copy is finished
		to release the spinlock held.
*/

import (
	"runtime/internal/atomic"
	"runtime/internal/sys"
	"unsafe"
)

// Prime numbers pre-generated for the interlocked iterator to use when determining randomized start position
var primes = [...]int{
	2, 3, 5, 7, 11, 13, 17, 19, 23, 29,
	31, 37, 41, 43, 47, 53, 59, 61, 67, 71,
	73, 79, 83, 89, 97, 101, 103, 107, 109, 113,
	127, 131, 137, 139, 149, 151, 157, 163, 167, 173,
	179, 181, 191, 193, 197, 199, 211, 223, 227, 229,
	233, 239, 241, 251, 257, 263, 269, 271, 277, 281,
	283, 293, 307, 311, 313, 317, 331, 337, 347, 349,
	353, 359, 367, 373, 379, 383, 389, 397, 401, 409,
	419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
	467, 479, 487, 491, 499, 503, 509, 521, 523, 541,
	547, 557, 563, 569, 571, 577, 587, 593, 599, 601,
	607, 613, 617, 619, 631, 641, 643, 647, 653, 659,
	661, 673, 677, 683, 691, 701, 709, 719, 727, 733,
	739, 743, 751, 757, 761, 769, 773, 787, 797, 809,
	811, 821, 823, 827, 829, 839, 853, 857, 859, 863,
	877, 881, 883, 887, 907, 911, 919, 929, 937, 941,
	947, 953, 967, 971, 977, 983, 991, 997, 1009, 1013,
	1019, 1021, 1031, 1033, 1039, 1049, 1051, 1061, 1063, 1069,
	1087, 1091, 1093, 1097, 1103, 1109, 1117, 1123, 1129, 1151,
	1153, 1163, 1171, 1181, 1187, 1193, 1201, 1213, 1217, 1223,
	1229, 1231, 1237, 1249, 1259, 1277, 1279, 1283, 1289, 1291,
	1297, 1301, 1303, 1307, 1319, 1321, 1327, 1361, 1367, 1373,
	1381, 1399, 1409, 1423, 1427, 1429, 1433, 1439, 1447, 1451,
	1453, 1459, 1471, 1481, 1483, 1487, 1489, 1493, 1499, 1511,
	1523, 1531, 1543, 1549, 1553, 1559, 1567, 1571, 1579, 1583,
	1597, 1601, 1607, 1609, 1613, 1619, 1621, 1627, 1637, 1657,
	1663, 1667, 1669, 1693, 1697, 1699, 1709, 1721, 1723, 1733,
	1741, 1747, 1753, 1759, 1777, 1783, 1787, 1789, 1801, 1811,
	1823, 1831, 1847, 1861, 1867, 1871, 1873, 1877, 1879, 1889,
	1901, 1907, 1913, 1931, 1933, 1949, 1951, 1973, 1979, 1987,
	1993, 1997, 1999, 2003, 2011, 2017, 2027, 2029, 2039, 2053,
	2063, 2069, 2081, 2083, 2087, 2089, 2099, 2111, 2113, 2129,
	2131, 2137, 2141, 2143, 2153, 2161, 2179, 2203, 2207, 2213,
	2221, 2237, 2239, 2243, 2251, 2267, 2269, 2273, 2281, 2287,
	2293, 2297, 2309, 2311, 2333, 2339, 2341, 2347, 2351, 2357,
	2371, 2377, 2381, 2383, 2389, 2393, 2399, 2411, 2417, 2423,
	2437, 2441, 2447, 2459, 2467, 2473, 2477, 2503, 2521, 2531,
	2539, 2543, 2549, 2551, 2557, 2579, 2591, 2593, 2609, 2617,
	2621, 2633, 2647, 2657, 2659, 2663, 2671, 2677, 2683, 2687,
	2689, 2693, 2699, 2707, 2711, 2713, 2719, 2729, 2731, 2741,
	2749, 2753, 2767, 2777, 2789, 2791, 2797, 2801, 2803, 2819,
	2833, 2837, 2843, 2851, 2857, 2861, 2879, 2887, 2897, 2903,
	2909, 2917, 2927, 2939, 2953, 2957, 2963, 2969, 2971, 2999,
	3001, 3011, 3019, 3023, 3037, 3041, 3049, 3061, 3067, 3079,
	3083, 3089, 3109, 3119, 3121, 3137, 3163, 3167, 3169, 3181,
	3187, 3191, 3203, 3209, 3217, 3221, 3229, 3251, 3253, 3257,
	3259, 3271, 3299, 3301, 3307, 3313, 3319, 3323, 3329, 3331,
	3343, 3347, 3359, 3361, 3371, 3373, 3389, 3391, 3407, 3413,
	3433, 3449, 3457, 3461, 3463, 3467, 3469, 3491, 3499, 3511,
	3517, 3527, 3529, 3533, 3539, 3541, 3547, 3557, 3559, 3571,
	3581, 3583, 3593, 3607, 3613, 3617, 3623, 3631, 3637, 3643,
	3659, 3671, 3673, 3677, 3691, 3697, 3701, 3709, 3719, 3727,
	3733, 3739, 3761, 3767, 3769, 3779, 3793, 3797, 3803, 3821,
	3823, 3833, 3847, 3851, 3853, 3863, 3877, 3881, 3889, 3907,
	3911, 3917, 3919, 3923, 3929, 3931, 3943, 3947, 3967, 3989,
	4001, 4003, 4007, 4013, 4019, 4021, 4027, 4049, 4051, 4057,
	4073, 4079, 4091, 4093, 4099, 4111, 4127, 4129, 4133, 4139,
	4153, 4157, 4159, 4177, 4201, 4211, 4217, 4219, 4229, 4231,
	4241, 4243, 4253, 4259, 4261, 4271, 4273, 4283, 4289, 4297,
	4327, 4337, 4339, 4349, 4357, 4363, 4373, 4391, 4397, 4409,
	4421, 4423, 4441, 4447, 4451, 4457, 4463, 4481, 4483, 4493,
	4507, 4513, 4517, 4519, 4523, 4547, 4549, 4561, 4567, 4583,
	4591, 4597, 4603, 4621, 4637, 4639, 4643, 4649, 4651, 4657,
	4663, 4673, 4679, 4691, 4703, 4721, 4723, 4729, 4733, 4751,
	4759, 4783, 4787, 4789, 4793, 4799, 4801, 4813, 4817, 4831,
	4861, 4871, 4877, 4889, 4903, 4909, 4919, 4931, 4933, 4937,
	4943, 4951, 4957, 4967, 4969, 4973, 4987, 4993, 4999, 5003,
	5009, 5011, 5021, 5023, 5039, 5051, 5059, 5077, 5081, 5087,
	5099, 5101, 5107, 5113, 5119, 5147, 5153, 5167, 5171, 5179,
	5189, 5197, 5209, 5227, 5231, 5233, 5237, 5261, 5273, 5279,
	5281, 5297, 5303, 5309, 5323, 5333, 5347, 5351, 5381, 5387,
	5393, 5399, 5407, 5413, 5417, 5419, 5431, 5437, 5441, 5443,
	5449, 5471, 5477, 5479, 5483, 5501, 5503, 5507, 5519, 5521,
	5527, 5531, 5557, 5563, 5569, 5573, 5581, 5591, 5623, 5639,
	5641, 5647, 5651, 5653, 5657, 5659, 5669, 5683, 5689, 5693,
	5701, 5711, 5717, 5737, 5741, 5743, 5749, 5779, 5783, 5791,
	5801, 5807, 5813, 5821, 5827, 5839, 5843, 5849, 5851, 5857,
	5861, 5867, 5869, 5879, 5881, 5897, 5903, 5923, 5927, 5939,
	5953, 5981, 5987, 6007, 6011, 6029, 6037, 6043, 6047, 6053,
	6067, 6073, 6079, 6089, 6091, 6101, 6113, 6121, 6131, 6133,
	6143, 6151, 6163, 6173, 6197, 6199, 6203, 6211, 6217, 6221,
	6229, 6247, 6257, 6263, 6269, 6271, 6277, 6287, 6299, 6301,
	6311, 6317, 6323, 6329, 6337, 6343, 6353, 6359, 6361, 6367,
	6373, 6379, 6389, 6397, 6421, 6427, 6449, 6451, 6469, 6473,
	6481, 6491, 6521, 6529, 6547, 6551, 6553, 6563, 6569, 6571,
	6577, 6581, 6599, 6607, 6619, 6637, 6653, 6659, 6661, 6673,
	6679, 6689, 6691, 6701, 6703, 6709, 6719, 6733, 6737, 6761,
	6763, 6779, 6781, 6791, 6793, 6803, 6823, 6827, 6829, 6833,
	6841, 6857, 6863, 6869, 6871, 6883, 6899, 6907, 6911, 6917,
	6947, 6949, 6959, 6961, 6967, 6971, 6977, 6983, 6991, 6997,
	7001, 7013, 7019, 7027, 7039, 7043, 7057, 7069, 7079, 7103,
	7109, 7121, 7127, 7129, 7151, 7159, 7177, 7187, 7193, 7207,
	7211, 7213, 7219, 7229, 7237, 7243, 7247, 7253, 7283, 7297,
	7307, 7309, 7321, 7331, 7333, 7349, 7351, 7369, 7393, 7411,
	7417, 7433, 7451, 7457, 7459, 7477, 7481, 7487, 7489, 7499,
	7507, 7517, 7523, 7529, 7537, 7541, 7547, 7549, 7559, 7561,
	7573, 7577, 7583, 7589, 7591, 7603, 7607, 7621, 7639, 7643,
	7649, 7669, 7673, 7681, 7687, 7691, 7699, 7703, 7717, 7723,
	7727, 7741, 7753, 7757, 7759, 7789, 7793, 7817, 7823, 7829,
	7841, 7853, 7867, 7873, 7877, 7879, 7883, 7901, 7907, 7919,
}

const (
	// must match value in ../cmd/compile/internal/gc/walk.go
	// MAXZERO is the size of the zero'd portion that must be returned when the requested element is not found in the map
	// yet it requires what is returned to be non-nil for compiler optimizations.
	MAXZERO = 1024

	// The number of buckets in the root bucketArray.
	DEFAULT_BUCKETS = 32
	// The number of slots (hash/key/value) in a bucketData.
	MAX_SLOTS = 8

	// The bucketHdr can safely be casted to a bucketArray
	ARRAY = 1 << 0
	// The bucketHdr is invalidated and needs to be reloaded (occurs during resizing or deletion)
	INVALID = 1 << 1

	// The bucketHdr lock is uncontested
	UNLOCKED = 0
	// Mask used to determine the lock-holder; Used when we are in a tight-loop, waiting for lock-holder to give up lock.
	// This will be mentioned once: We reset the backoff variables when the lock-holder relinquishes the lock to prevent excessive spinning.
	LOCKED_MASK = ^uintptr(0x3)

	// Hash value signifying that the hash is not in use.
	EMPTY = 0
	// Used to obtain the top 8-bits of a hash. I.E: hash >> HASH_SHIFT
	HASH_SHIFT = sys.PtrSize*8 - 8

	// The minimum number of CPU cycles we spin during the tight-spin waiting for the lock holder to change.
	MIN_SPIN_CYCLES = 40
	// The number of CPU cycles we go up by on each iteration
	SPIN_INCREMENT = 10

	// After this many spins, we yield (remember Goroutine context switching only requires a switch in SP/PC and DX register and is lightning fast)
	GOSCHED_AFTER_SPINS = 9
	// After this many spins, we backoff (time.Sleep unfortunately has us park on a semaphore, but if we spin this many times, it's not a huge deal...)
	// Also as well, due to this, when deadlocks occur they are easier to identify since the CPU sinks to 0% rather than infinitely at 100%
	SLEEP_AFTER_SPINS = 10

	// Default backoff; 1 microsecond
	DEFAULT_BACKOFF = 1024
	// Maximum backoff; 1 milliseconds
	MAX_BACKOFF = 1000000

	// Signifies the key at the corresponding offset has been deleted and should be zero'd after the interlocked block
	KEY_DELETED = uintptr(1 << 0)

	// The bit that gets set when we finished wrapping around the bucketArray is the very last one.
	WRAPPED = uint32(1 << 31)

	// During pre-expansion, this is the number of buckets per potential Goroutine accessing the map in parallel
	BUCKET_PER_GOROUTINE = int64(0)

	// See hashmap.go, this obtains a properly aligned offset to the data
	// Is used to obtain the array of keys and values at the end of the runtime representation of the bucketData type.
	cdataOffset = unsafe.Offsetof(struct {
		b bucketData
		v int64
	}{}.v)
)

// Returned when no element is found, as we are allowed to return nil. See hashmap.go...
var DUMMY_RETVAL [MAXZERO]byte

/*
	Header and descriptor for a bucket. Intermediate type for both bucketArray and bucketData,
	that, not only describes the bucket (type and number of occupied buckets/elements respectively),
	but also functions as a backlink to provide a pseudo-linked list.
*/
type bucketHdr struct {
	// Describes both the lock state as well as the type of this bucket.
	// If it is INVALID, the Goroutine requesting access to this bucket needs to refresh (to parent.buckets[parentIdx])
	// If it is ARRAY, we are a bucketArray, otherwise bucketData
	lock uintptr
	// Number of elements in this bucketHdr
	count uint32
	// Index we are on inside of our parent.
	parentIdx uint32
	// The parent bucketArray we belong to; nil if we are the root.
	parent *bucketArray
}

/*
   bucketArray is the body that keeps track of an array of bucketHdr's, that may point to either bucketData or even other bucketArrays.
   It's seed is unique relative to other bucketArray's to prevent excess collisions during hashing and reduce possible contention.
   It keeps track of the location of the bucketHdr that pointed to this, for O(1) navigation during iteration.
   Can be casted to and from bucketHdr.
*/
type bucketArray struct {
	// Fields embedded from bucketHdr; for complexity reasons, we can't actually embed the type in the runtime (because we would have to also do so in compiler)
	lock      uintptr
	count     uint32
	parentIdx uint32
	parent    *bucketArray

	// Seed is different for each bucketArray to ensure that the re-hashing resolves to different indice
	seed uint32
	// Slice of bucketHdr's.
	buckets []*bucketHdr
}

/*
   bucketData is the actual bucket itself, containing MAX_SLOTS data slots (hash/key/value) for elements it holds.
   It can be casted to and from bucketHdr.
   It's key and value slots are only accessible through unsafe pointer arithmetic.
*/
type bucketData struct {
	// Fields embedded from bucketHdr; for complexity reasons, we can't actually embed the type in the runtime (because we would have to also do so in compiler)
	lock      uintptr
	count     uint32
	parentIdx uint32
	parent    *bucketArray

	// Hash of the key-value corresponding to this index. If it is 0, it is empty. Top byte to reduce overall size.
	tophash [MAX_SLOTS]uint8
	// It's key and value slots are below, and would appear as such if the runtime supported generics...
	/*
	   key [MAX_SLOTS]keyType
	   val [MAX_SLOTS]valType
	*/
}

/*
	concurrentMap is the header which contains the root bucket which contains all data. It is the entry-point into the map.
*/
type concurrentMap struct {
	// Root bucket.
	root bucketArray
}

/*
	concurrentIterator is our version of the iterator header used in hashmap.go.

	It keeps track of where it is in the map, and where we should stop at/wrap to for randomized iteration.
	For snapshot iteration, the data field is used to iterate over a snapshot. For interlocked iteration, it's 'g' holds the locked bucket
	and is retrieved from that.

	The offset keeps track of it's current offset inside of the data it holds.
*/
type concurrentIterator struct {
	// The current index we are on
	idx uint32
	// Offset we are inside of data we are iterating over.
	offset uint32
	// Flags which keep track of the state of the iterator.
	flags uint32
	// Determines our current depth we have recursed from the root; used specifically when we are iterating through all skipped buckets
	depth uint32
	// The bucketArray we currently are working on
	arr *bucketArray
	// Used to keep track of the randomized start position we wrap up to.
	startIdx []uint32
	// Cached 'g' for faster access
	g *g
	// Slice of all buckets we skip over due to not being able to acquire the lock fast enough (and reduce overall convoying).
	skippedBuckets []*bucketHdr
}

///////////////////////////////
//			HELPERS			//
/////////////////////////////

/*
	Helper function to get rid of the interlockedInfo associated with this map.
*/
func interlocked_release(h *hmap) {
	g := getg()
	for idx, info := range g.interlockedData {
		// This is the one we're looking for...
		if info.cmap == h.chdr {
			// To get rid of the header, all we have to do is ensure that all valid interlockedInfo are not
			// at the end of the structure, as we are going to be popping it off.
			end := len(g.interlockedData) - 1
			if end != idx {
				g.interlockedData[idx] = g.interlockedData[end]
			}
			// Pop
			g.interlockedData = g.interlockedData[:len(g.interlockedData)-1]
			return
		}
	}
	panic("Interlocked info not found for map!!!")
}

/*
	Obtains the pointer to the key slot at the requested offset.
*/
func (data *bucketData) key(t *maptype, idx uint32) unsafe.Pointer {
	// Cast data to unsafe.Pointer to bypass Go's type system
	rawData := unsafe.Pointer(data)
	// The array of keys are located at the beginning of cdataOffset, and is contiguous up to MAX_SLOTS
	keyOffset := uintptr(rawData) + uintptr(cdataOffset)
	// Now the key at index 'idx' is located at idx * t.keysize
	ourKeyOffset := keyOffset + uintptr(idx)*uintptr(t.keysize)
	return unsafe.Pointer(ourKeyOffset)
}

/*
	Obtains the pointer to the value slot at the requested offset.
*/
func (data *bucketData) value(t *maptype, idx uint32) unsafe.Pointer {
	// Cast data to unsafe.Pointer to bypass Go's type system
	rawData := unsafe.Pointer(data)
	// The array of keys are located at the beginning of cdataOffset, and is contiguous up to MAX_SLOTS
	keyOffset := uintptr(rawData) + uintptr(cdataOffset)
	// The array of values are located at the end of the array of keys, located at MAX_SLOTS * t.keysize
	valueOffset := keyOffset + MAX_SLOTS*uintptr(t.keysize)
	// Now the value at index 'idx' is located at idx * t.valuesize
	ourValueOffset := valueOffset + uintptr(idx)*uintptr(t.valuesize)
	return unsafe.Pointer(ourValueOffset)
}

/*
	Assigns into the data slot the passed information at the requested index (hash/key/value)
*/
func (data *bucketData) assign(t *maptype, idx uint32, hash uintptr, key, value unsafe.Pointer) {
	k := data.key(t, idx)
	v := data.value(t, idx)

	if t.indirectkey {
		kmem := newobject(t.key)
		*(*unsafe.Pointer)(k) = kmem
		k = kmem
	}
	if t.indirectvalue {
		vmem := newobject(t.elem)
		*(*unsafe.Pointer)(v) = vmem
		v = vmem
	}

	// Copies memory in a way that it updates the GC to know objects pointed to by this copy should not be collected
	if key != nil {
		typedmemmove(t.key, k, key)
	}

	if value != nil {
		typedmemmove(t.elem, v, value)
	}

	tophash := uint8(hash >> HASH_SHIFT)
	// Top COULD be 0
	if tophash == 0 {
		tophash += 1
	}
	data.tophash[idx] = tophash
}

/*
	Updates the requested key and value at the requested index. Note that it assumes that the index is correct and corresponds to the key passed.
*/
func (data *bucketData) update(t *maptype, idx uint32, key, value unsafe.Pointer) {
	v := data.value(t, idx)

	// Indirect Key and Values need some indirection
	if t.indirectvalue {
		v = *(*unsafe.Pointer)(v)
	}

	// If we are required to update key, do so
	if t.needkeyupdate {
		k := data.key(t, idx)
		if t.indirectkey {
			k = *(*unsafe.Pointer)(k)
		}
		typedmemmove(t.key, k, key)
	}

	typedmemmove(t.elem, v, value)
}

/*
	Used during debugging to profile total size of the map, as well as the actual nesting.
*/
//go:linkname profile_map reflect.profile_map
func profile_map(h *hmap) {
	cmap := (*concurrentMap)(h.chdr)
	arr := &cmap.root
	var idx uint32
	var depth, nData, nArr int
	nArrPtrs := DEFAULT_BUCKETS
	// Arbitrary max; if we ever go out of bounds, we're in trouble!
	depthMap := make([]uint64, 1)
next:
	arrAtDepth := depthMap[depth] >> 32
	dataAtDepth := depthMap[depth] & uint64(^uint32(0))

	if idx == uint32(len(arr.buckets)) {
		if arr.parent == nil {
			// Dump
			println("\rTotal Data:", nData, ";Total Array:", nArr)
			for idx, _ := range depthMap {
				arrAtDepth = depthMap[idx] >> 32
				dataAtDepth = depthMap[idx] & uint64(^uint32(0))
				println("Depth:", idx, ";nData:", dataAtDepth, "nArray:", arrAtDepth)
			}
			println("Sizeof: bucketData(EList)=", uint64(unsafe.Sizeof(bucketData{}))+uint64(16*8))
			println("Sizeof: bucketArray(PList)=", unsafe.Sizeof(bucketArray{}))
			println("Size of all bucketData(EList)=", (uint64(unsafe.Sizeof(bucketData{}))+uint64(16*8))*uint64(nData))
			println("Size of all bucketArray(PList)=", uint64(unsafe.Sizeof(bucketArray{}))*uint64(nArr)+uint64(sys.PtrSize*nArrPtrs))
			return
		} else {
			depth--
			idx = arr.parentIdx
			arr = arr.parent
			idx++
			goto next
		}
	}

	hdr := arr.buckets[idx]
	idx++
	if hdr == nil {
		goto next
	}

	if hdr.lock == ARRAY {
		nArr++
		arrAtDepth++
		depthMap[depth] = (arrAtDepth << 32) | dataAtDepth
		depth++
		if len(depthMap) < (depth + 1) {
			depthMap = append(depthMap, 0)
		}
		arr = (*bucketArray)(unsafe.Pointer(hdr))
		nArrPtrs += len(arr.buckets)
		idx = 0
	} else {
		nData++
		dataAtDepth++
		depthMap[depth] = (arrAtDepth << 32) | dataAtDepth
	}

	// print("\rData:", nData, ";Array:", nArr, ";nPtrs:", nArrPtrs)
	goto next
}

///////////////////////////////////
//			ITERATION			//
//////////////////////////////////

/*
	Initializes the iterator to a randomized start position, and then obtains the
	first element to be iterated over.
*/
func cmapiterinit(t *maptype, h *hmap, it *hiter) {
	// Clear pointer fields so garbage collector does not complain.
	it.key = nil
	it.value = nil
	it.t = nil
	it.h = nil
	it.buckets = nil
	it.bptr = nil
	it.overflow[0] = nil
	it.overflow[1] = nil
	it.citerHdr = nil

	// You cannot iterate a nil or empty map
	if h == nil || atomic.Load((*uint32)(unsafe.Pointer(&h.count))) == 0 {
		it.key = nil
		it.value = nil
		return
	}

	it.t = t
	it.h = h

	cmap := (*concurrentMap)(h.chdr)
	arr := &cmap.root
	citer := (*concurrentIterator)(newobject(t.concurrentiterator))
	it.citerHdr = unsafe.Pointer(citer)
	citer.arr = arr

	// By setting offset to MAX_SLOTS, it allows it to bypass the findKeyValue portion without modification
	citer.offset = MAX_SLOTS

	// Push a new interlockedInfo on the 'g's stack, and cache it for faster access
	g := getg()
	data := (*interlockedInfo)(newobject(t.interlockedinfo))
	data.cmap = h.chdr
	g.interlockedData = append(g.interlockedData, data)
	citer.g = g

	// Randomized root start index is a random prime, modulo the number of root buckets
	rootStartIdx := uint32(primes[fastrand1()%uint32(len(primes))] % DEFAULT_BUCKETS)
	citer.startIdx = make([]uint32, 1)
	citer.startIdx[0] = rootStartIdx
	citer.idx = uint32((rootStartIdx + 1) % DEFAULT_BUCKETS)

	// Obtain first element.
	cmapiternext(it)
}

/*
	Yields the next key/value pair in the map. While we have a bucket, we maintain exclusive
	access (meaning, we hold the spinlock) until we finish processing that bucket. If we come
	across a nested bucketArray, we traverse and begin at a random start position to reduce chances
	of convoying. As well, when we come across a locked bucket, we skip over it to process
	it later (during polling stage).
*/
func cmapiternext(it *hiter) {
	var data *bucketData
	var hdr *bucketHdr
	var key, value unsafe.Pointer
	var spins int64
	var idx, offset, startIdx uint32

	citer := (*concurrentIterator)(it.citerHdr)
	info := (*interlockedInfo)(citer.g.interlockedData[len(citer.g.interlockedData)-1])
	t := it.t
	g := citer.g
	gptr := uintptr(unsafe.Pointer(g))

	// Find the next key-value element. It assumes that if citer.offset < MAX_SLOTS, that citer.info.hdr actually holds a valid header.
	// This is jumped to during iteration when we acquire a valid bucketHdr containing any elements and need to iterate over that bucket.
findKeyValue:
	offset = citer.offset
	citer.offset++

	// Grab the data we are currently on; During initialization, data will be nil, so skip to next.
	data = (*bucketData)(unsafe.Pointer(info.hdr))
	if data == nil {
		citer.offset = 0
		goto next
	}

	// If there is more to find, do so
	if offset < MAX_SLOTS {
		// Ensure we do not skip the first KEY_DELETED bit
		if offset > 0 {
			// Shift over by one bit so KEY_DELETED bit is unique to each index.
			info.flags = info.flags << 1
		}

		// If this cell is empty, loop again
		if data.tophash[offset] == EMPTY {
			goto findKeyValue
		}

		// The key and values are present, but perform necessary indirection
		key = data.key(t, offset)
		if t.indirectkey {
			key = *(*unsafe.Pointer)(key)
		}
		value = data.value(t, offset)
		if t.indirectvalue {
			value = *(*unsafe.Pointer)(value)
		}

		// Set the iterator's data and we're done
		it.key = key
		it.value = value

		info.key = data.key(t, offset)
		info.value = data.value(t, offset)
		info.hash = &data.tophash[offset]
		return
	}

	// If the offset == MAX_SLOTS, then we exhausted this bucketData, reset offset for next one
	citer.offset = 0

	// Since we maintain information used during interlocked iteration, its our job to also clean that up.
	// When we delete an element during iteration, it's key and hash are not zero'd/cleared to allow the user to
	// reassign to them later. Since we need to keep track of which keys are deleted, we encode them into the flags
	// field and using it as a bitmap.

	// If flags == 0, then none of the KEY_DELETED bits are set, so we can easily just proceed.
	if info.flags != 0 {
		// Check each bit
		for bit, idx := uint32(1), MAX_SLOTS-1; idx >= 0; bit, idx = bit<<1, idx-1 {
			// If the bit is set, the key and hash need to be zero'd.
			if (info.flags & uintptr(bit)) != 0 {
				memclr(unsafe.Pointer(data.key(t, uint32(idx))), uintptr(t.keysize))
				data.tophash[idx] = EMPTY
			}
		}

		// Clear the info flags
		info.flags = 0
	}

	// Check for the case when we deleted all elements in this bucket, and if we did, invalidate and delete it
	if data.count == 0 {
		// Invalidate and release the bucket (as it is being deleted)
		sync_atomic_StorePointer((*unsafe.Pointer)(unsafe.Pointer(&data.parent.buckets[data.parentIdx])), nil)
		atomic.Storeuintptr(&data.lock, INVALID)

		// Also decrement number of buckets
		atomic.Xadd(&data.parent.count, -1)
	} else {
		// Otherwise, just release the lock on the bucket
		atomic.Storeuintptr(&data.lock, UNLOCKED)
	}

	// Zero all fields to help GC
	info.hdr = nil
	info.key = nil
	info.value = nil
	info.hash = nil

	// Find the next bucketData if there is one
next:
	startIdx = citer.startIdx[len(citer.startIdx)-1]
	idx = citer.idx

	// If we have WRAPPED around the bucketArray, we are finished iterating it.
	if startIdx == WRAPPED {
		// We are not at the root, so go back one.
		if citer.depth > 0 {
			// Go back one
			citer.idx = citer.arr.parentIdx
			citer.arr = citer.arr.parent

			// Increment idx by one to move on to next bucketHdr
			citer.idx++

			citer.depth--
			citer.startIdx = citer.startIdx[:len(citer.startIdx)-1]

			goto next
		} else {
			// This is the root, so we have no more to process; check any skipped buckets.
			goto pollSkippedBuckets
		}
	} else if idx == startIdx {
		// At this point, we are on the last bucket in this bucketArray and have already wrapped around
		// Flag it so we don't continue after we are finished processing this bucket
		citer.startIdx[len(citer.startIdx)-1] = WRAPPED
	} else if idx == uint32(len(citer.arr.buckets)) {
		// At this point, we hit the last cell in the bucketArray but have not wrapped yet
		citer.idx = 0
		// In the case citer.startIdx == 0, it would proceed to process it as if it wasn't the last bucket.
		// Hence we must jump back to next.
		goto next
	}

	// Obtain header (and forward index by one for next iteration)
	hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&citer.arr.buckets[idx])))
	citer.idx++

	// Read ahead of time if we should skip.
	if hdr == nil || atomic.Load(&hdr.count) == 0 {
		goto next
	}

	for {
		// Reset backoff variables
		spins = 0

		lock := atomic.Loaduintptr(&hdr.lock)

		// If the state of the bucket is INVALID, then either it's been deleted or been converted into an ARRAY; Reload and try again
		if lock == INVALID {
			// Reload hdr, since what it was pointed to has changed; idx - 1 because we incremented above
			hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&citer.arr.buckets[idx])))
			// If the hdr was deleted, then the data we're trying to find isn't here anymore (if it was at all).
			// hdr.count == 0 iff another Goroutine has created a new bucketData during a 'mapassign' but has not yet finished it's assignment.
			// In this case, there's still nothing here for us.
			if hdr == nil || atomic.Load(&hdr.count) == 0 {
				goto next
			}
			// Loop again.
			continue
		}

		// If it's recursive, recurse and find new bucket
		if lock == ARRAY {
			citer.arr = (*bucketArray)(unsafe.Pointer(hdr))

			size := len(citer.arr.buckets)
			randStart := uint32(primes[fastrand1()%uint32(len(primes))] % size)
			citer.startIdx = append(citer.startIdx, randStart)
			citer.idx = uint32((randStart + 1) % uint32(size))

			citer.depth++

			goto next
		}

		// Acquire lock on bucket
		if lock == UNLOCKED {
			// Attempt to acquire
			if atomic.Casuintptr(&hdr.lock, UNLOCKED, gptr) {
				break
			}
			continue
		}

		// If we already own the lock, then we somehow forget to release the lock and the map is in a bad state.
		if lock == gptr {
			throw("Unexpected: Discovered already-owned lock while iterating...")
			break
		}

		// During iteration, we do not backoff to reduce the effects of lock convoying.
		// Instead we skip this bucket and process it later.
		citer.skippedBuckets = append(citer.skippedBuckets, citer.arr.buckets[idx])
		goto next
	}

	info.hdr = hdr

	// We have the data we are looking for.
	goto findKeyValue

	// Called to poll thorugh any skipped buckets
pollSkippedBuckets:
	// Reset backoff variables
	spins = 0
	backoff := DEFAULT_BACKOFF

	// At this point, we are iterating through any and all skipped buckets, polling for ones that are available.
	for {
		// Since we cannot remove the processed buckets, we need to ensure that we are actually doing work.
		// If we find all nil buckets, we are finished.
		doneProcessing := true
		for idx, hdr := range citer.skippedBuckets {
			// If the pointer is nil, we already processed it,
			if hdr == nil {
				continue
			}

			lock := atomic.Loaduintptr(&hdr.lock)

			// In the case where it is marked INVALID, we reload the bucket and poll on it next time around
			if lock == INVALID {
				citer.skippedBuckets[idx] = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&hdr.parent.buckets[hdr.parentIdx])))
				doneProcessing = false
				continue
			}

			// There is no data here (yet), dispose of it.
			if atomic.Load(&hdr.count) == 0 {
				citer.skippedBuckets[idx] = nil
				continue
			}

			// In the easier case wherein the hdr has ARRAY bit flagged, we can allow 'next' label to take care of it.
			// Randomize start iteration as well.
			if lock == ARRAY {
				citer.arr = (*bucketArray)(unsafe.Pointer(hdr))

				// Randomize start position
				size := len(citer.arr.buckets)
				randStart := uint32(primes[fastrand1()%uint32(len(primes))] % size)
				citer.startIdx = append(citer.startIdx, randStart)
				citer.idx = uint32((randStart + 1) % uint32(size))

				// We are processing it, so make sure we nil it out.
				citer.skippedBuckets[idx] = nil

				goto next
			}

			// In this case, we know that lock is not invalid (yet) nor is it ARRAY (yet), so we do a simple test for if it is UNLOCKED.
			// Once again, this is polling, so we don't do a tight spin or anything else either.
			if lock == UNLOCKED {
				// Test-And-Set
				if atomic.Casuintptr(&hdr.lock, UNLOCKED, gptr) {
					info.hdr = hdr

					// We are processing this hdr, so nil it out
					citer.skippedBuckets[idx] = nil

					// Begin processing the interlocked bucket
					goto findKeyValue
				}
			}

			if lock == gptr {
				throw("Unexpected: lock == gptr, iterated over a skipped bucket we currently own!!!")
			}

			// At this point, this bucket cannot be processed yet
			doneProcessing = false
		}

		if doneProcessing {
			break
		}

		// Handle polliing over buckets with some backoff.
		if spins < GOSCHED_AFTER_SPINS {
			procyield(uint32(MIN_SPIN_CYCLES + (spins * SPIN_INCREMENT)))
		} else if spins < SLEEP_AFTER_SPINS {
			Gosched()
		} else {
			timeSleep(int64(backoff))

			// ≈1ms
			if backoff < MAX_BACKOFF {
				backoff *= 2
			}
		}
		spins++
	}

	// If we make it this far, we've processed everything.
	it.key = nil
	it.value = nil

	interlocked_release(it.h)
}

///////////////////////////////
//			CORE			//
/////////////////////////////

/*
	Creates a concurrent map. The map will expand it's nesting appropriately depending on the number of
	requested elements supplied by the user. Currently, 'concurrencyLevel' is unused.

	Syntax:

		make(map[keyType]valType, NUM_ELEMS, NUM_CONCURRENCY)
*/
func makecmap(t *maptype, hint int64, h *hmap, bucket unsafe.Pointer, concurrencyLevel int64) *hmap {
	// Initialize the hashmap if needed
	if h == nil {
		h = (*hmap)(newobject(t.hmap))
	}

	// Initialize and allocate our concurrentMap
	cmap := (*concurrentMap)(newobject(t.concurrentmap))
	cmap.root.buckets = make([]*bucketHdr, DEFAULT_BUCKETS)
	cmap.root.seed = fastrand1()
	cmap.root.lock = ARRAY

	// We need enough buckets to hold all elements
	maxHint := hint / MAX_SLOTS
	gomaxprocs := int64(GOMAXPROCS(0))
	// As a rule of thumb, we if concurrencyLevel < GOMAXPROCS, we raise it to GOMAXPROCS anyway.
	if concurrencyLevel < gomaxprocs {
		concurrencyLevel = gomaxprocs
	}

	// We also want to allow at least BUCKET_PER_GOROUTINE buckets per Goroutine to use (to maximize potential concurrency)
	if int64(concurrencyLevel)*BUCKET_PER_GOROUTINE > maxHint {
		maxHint = concurrencyLevel * int64(BUCKET_PER_GOROUTINE)
	}

	// The root buckets are of size DEFAULT_BUCKETS, but the child buckets are always twice the size as the previous.
	// What this means is that the number of buckets is DEFAULT_BUCKETS * (2 * DEFAULT_BUCKETS) * (3 * DEFAULT_BUCKETS) * ... * (N * DEFAULT_BUCKETS)
	nBuckets := int64(32)
	nestingLevel := uint32(0)
	for i := uint32(0); nBuckets < maxHint; i++ {
		nestingLevel++
		nBuckets *= 32 * (1 << nestingLevel)
	}

	// Begin expansion out to the nestingLevel; We stop pre-expansion at level 2 for simplicity sake
	// (and that at this level it can take 2 Million elements, more than enough, plus with the high memory overhead)
	if nestingLevel > 0 {
		cmap.root.count = DEFAULT_BUCKETS
		// Fill out nesting level 1
		for i := uint32(0); i < DEFAULT_BUCKETS; i++ {
			arrL1 := (*bucketArray)(newobject(t.bucketarray))
			arrL1.lock = ARRAY
			arrL1.seed = fastrand1()
			arrL1.buckets = make([]*bucketHdr, DEFAULT_BUCKETS*2)
			arrL1.parent = &cmap.root
			arrL1.parentIdx = i
			arrL1.count = DEFAULT_BUCKETS * 2
			cmap.root.buckets[i] = (*bucketHdr)(unsafe.Pointer(arrL1))

			if nestingLevel > 1 {
				// Fill out nesting level 2
				for j := uint32(0); j < DEFAULT_BUCKETS*2; j++ {
					arrL2 := (*bucketArray)(newobject(t.bucketarray))
					arrL2.lock = ARRAY
					arrL2.seed = fastrand1()
					arrL2.buckets = make([]*bucketHdr, DEFAULT_BUCKETS*4)
					arrL2.parent = arrL1
					arrL2.parentIdx = j
					arrL1.buckets[j] = (*bucketHdr)(unsafe.Pointer(arrL2))
				}
			}
		}
	}

	h.chdr = unsafe.Pointer(cmap)
	h.flags = 8 // CONCURRENT flag is non exported.

	return h
}

/*
	Inserts an element into the map, updating it if it is already present. If a bucket that a key hashed to is nil,
	it will attempt to do a wait-free creation of that bucket (locked) and insert it immediately. If the bucket is full,
	it will rehash it into a new bucketArray and atomically update the parent's reference to point to the new bucketArray,
	marking the old (previously full) bucketData as INVALID.

	Syntax:

		map[key] = value
*/
func cmapassign1(t *maptype, h *hmap, key unsafe.Pointer, val unsafe.Pointer) {
	// g := getg().m.curg
	// println("g #", getg().goid, ": cmapassign1")

	g := getg()
	// If we are currently interlocked on this map, take the fast path.
	for _, info := range g.interlockedData {
		if info.cmap == h.chdr {
			cmapassign_interlocked(t, info, h, key, val)
			return
		}
	}

	cmap := (*concurrentMap)(h.chdr)
	arr := &cmap.root
	var hash, idx, spins uintptr
	var backoff int64
	var hdr *bucketHdr
	gptr := uintptr(unsafe.Pointer(g))
	// println("Root length:", len(arr.buckets))

	// Finds the bucket associated with the key's hash; if it is recursive we jump back to here.
next:
	// Obtain the hash, index, and bucketHdr.
	hash = t.key.alg.hash(key, uintptr(arr.seed))
	idx = hash % uintptr(len(arr.buckets))
	hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
	setByUs := false

	// If hdr is nil, then no bucketData has been created yet. Do a wait-free creation of bucketData;
	for hdr == nil {
		// Note that bucketData has the same first 3 fields as bucketHdr, and can be safely casted
		newHdr := (*bucketHdr)(newobject(t.bucketdata))
		// Since we're setting it, may as well attempt to acquire lock and fill out fields
		newHdr.lock = gptr
		newHdr.parent = arr
		newHdr.parentIdx = uint32(idx)
		// If we fail, then some other Goroutine has already placed theirs.
		if atomic.Casp1((*unsafe.Pointer)(unsafe.Pointer(&arr.buckets[idx])), nil, unsafe.Pointer(newHdr)) {
			// If we succeed, then we own this bucket and need to keep track of it
			g.releaseBucket = unsafe.Pointer(newHdr)
			// Also increment count of buckets
			atomic.Xadd(&arr.count, 1)
			setByUs = true
		}
		// Reload hdr
		hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
	}

	// Attempt to acquire lock
	for {
		// If we set the lock, we skip the locking part all together
		if setByUs {
			break
		}
		// Reset backoff variables
		spins = 0
		backoff = DEFAULT_BACKOFF

		lock := atomic.Loaduintptr(&hdr.lock)

		// If the state of the bucket is INVALID, then either it's been deleted or been converted into an ARRAY; Reload and try again
		if lock == INVALID {
			// Reload hdr, since what it was pointed to has changed
			hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
			// If the hdr was deleted, then attempt to create a new one and try again
			for hdr == nil {
				// Note that bucketData has the same first 5 fields as bucketHdr, and can be safely casted
				newHdr := (*bucketHdr)(newobject(t.bucketdata))
				// Since we're setting it, may as well attempt to acquire lock and fill out fields
				newHdr.lock = gptr
				newHdr.parent = arr
				newHdr.parentIdx = uint32(idx)
				// If we fail, then some other Goroutine has already placed theirs.
				if atomic.Casp1((*unsafe.Pointer)(unsafe.Pointer(&arr.buckets[idx])), nil, unsafe.Pointer(newHdr)) {
					// If we succeed, then we own this bucket and need to keep track of it
					g.releaseBucket = unsafe.Pointer(newHdr)
					// Also increment count of buckets
					atomic.Xadd(&arr.count, 1)
					setByUs = true
				}
				hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
			}
			// Loop again.
			continue
		}

		// If it's recursive, try again on new bucket
		if lock == ARRAY {
			// println("Old Array Length:", len(arr.buckets))
			arr = (*bucketArray)(unsafe.Pointer(hdr))
			// println("New Array Length:", len(arr.buckets))
			goto next
		}

		// If we hold the lock, something went wrong
		if lock == gptr {
			throw("Reacquired lock on bucket we currently own: Failed invariant!")
		}

		// If the lock is uncontested
		if lock == UNLOCKED {
			// Attempt to acquire
			if atomic.Casuintptr(&hdr.lock, UNLOCKED, gptr) {
				g.releaseBucket = unsafe.Pointer(hdr)
				// println("...g # ", g.goid, ": Acquired lock")
				break
			}
			continue
		}

		// Keep track of the current lock-holder
		holder := lock & LOCKED_MASK

		// Tight-spin until the current lock-holder releases lock
		for {
			done := false
			for i := 0; i < GOSCHED_AFTER_SPINS; i, spins = i+1, spins+1 {
				// We test the lock on each iteration
				lock = atomic.Loaduintptr(&hdr.lock)
				// If the previous lock-holder released the lock, attempt to acquire again.
				if lock != holder {
					if spins > 100000 {
						println("...g # ", g.goid, ": Spins:", spins, ", Backoff:", backoff)
					}
					done = true
					break
				}

				procyield(uint32(MIN_SPIN_CYCLES))
			}
			if done {
				break
			}
			Gosched()

			if spins > 1000000 {
				println("...g # ", g.goid, ": Function: cmapassign, Crash Dump: {")
				println("holder: ", holder)
				println("lock: ", lock)
				println("gptr: ", gptr)
				println("hdr: {")
				println("\taddress: ", hdr)
				println("\tcount: ", hdr.count)
				println("\tlock: ", hdr.lock)
				println("\tparent: {")
				println("\t\taddress: ", hdr.parent)
				println("\t\tlock: ", hdr.parent.lock)
				println("\t\tcount: ", hdr.parent.count)
				println("\t\tlen: ", len((*bucketArray)(unsafe.Pointer(hdr.parent)).buckets))
				println("\t}")
				println("\tparentIdx: ", hdr.parentIdx)
				println("}")

				throw("Deadlock Detected!")
			}
		}
	}

	data := (*bucketData)(unsafe.Pointer(hdr))
	count := atomic.Load(&hdr.count)
	firstEmpty := -1

	// In the special case that hdr.count == 0, then the bucketData is empty and we should just add the data immediately.
	if count == 0 {
		data.assign(t, 0, hash, key, val)
		atomic.Store(&hdr.count, 1)
		atomic.Xadd((*uint32)(unsafe.Pointer(&h.count)), 1)
		return
	}

	// Otherwise, we must scan all hashes to find a matching hash; if they match, check if they are equal
	for i, cnt := uint32(0), count; i < MAX_SLOTS && cnt > 0; i++ {
		tophash := uint8(hash >> HASH_SHIFT)
		// Top COULD be 0
		if tophash == 0 {
			tophash += 1
		}

		currHash := data.tophash[i]
		if currHash == EMPTY {
			// Keep track of the first empty so we know what to assign into if we do not find a match
			if firstEmpty == -1 {
				firstEmpty = int(i)
			}
			continue
		}
		cnt--

		// If the hash matches, check to see if keys are equal
		if tophash == currHash {
			otherKey := data.key(t, i)

			// If they are equal, update...
			if t.key.alg.equal(key, otherKey) {
				data.update(t, i, key, val)
				return
			}
		}
	}

	// If firstEmpty is still -1 and the bucket is full, that means we did not find any empty slots, and should convert immediate
	if firstEmpty == -1 && count == MAX_SLOTS {
		// println("g #", getg().goid, ": Resizing...")
		// Allocate and initialize
		// println("len(arr.buckets) = ", len(arr.buckets))
		newArr := (*bucketArray)(newobject(t.bucketarray))
		// println("len(arr.buckets) = ", len(arr.buckets))
		newArr.lock = ARRAY
		// println("sizeof bucketHdr =", unsafe.Sizeof(bucketHdr{}), ";sizeof bucketData =", unsafe.Sizeof(bucketData{}), ";sizeof bucketArray =", unsafe.Sizeof(bucketArray{}))
		// println("compiler_sizeof bucketHdr =", t.buckethdr.size, ";compiler_sizeof bucketData =", t.bucketdata.size, ";compiler_sizeof bucketArray =", t.bucketarray.size)
		// println("arr - newArr =", uintptr(unsafe.Pointer(arr))-uintptr(unsafe.Pointer(newArr)))
		// println("len(arr.buckets) = ", len(arr.buckets))
		newArr.buckets = make([]*bucketHdr, len(arr.buckets)*2)
		// println("len(arr.buckets) = ", len(arr.buckets))
		// println("len(newArr.buckets) = ", len(newArr.buckets))
		newArr.seed = fastrand1()
		newArr.parent = arr
		newArr.parentIdx = uint32(idx)

		// Rehash and move all key-value pairs
		for i := uint32(0); i < MAX_SLOTS; i++ {
			k := data.key(t, i)
			v := data.value(t, i)

			// Rehash the key to the new seed
			newHash := t.key.alg.hash(k, uintptr(newArr.seed))
			newIdx := newHash % uintptr(len(newArr.buckets))
			newHdr := newArr.buckets[newIdx]
			newData := (*bucketData)(unsafe.Pointer(newHdr))

			// Check if the bucket is nil, meaning we haven't allocated to it yet.
			if newData == nil {
				newArr.buckets[newIdx] = (*bucketHdr)(newobject(t.bucketdata))
				newArr.count++

				newData = (*bucketData)(unsafe.Pointer(newArr.buckets[newIdx]))
				newData.assign(t, 0, newHash, k, v)
				newData.count++
				newData.parent = newArr
				newData.parentIdx = uint32(newIdx)

				continue
			}

			// If it is not nil, then we must scan for the first non-empty slot
			for j := uint32(0); j < MAX_SLOTS; j++ {
				currHash := newData.tophash[j]
				if currHash == EMPTY {
					newData.assign(t, j, newHash, k, v)
					newData.count++
					break
				}
			}
		}

		// Now dispose of old data and update the header's bucket; We have to be careful to NOT overwrite the header portion
		memclr(add(unsafe.Pointer(data), unsafe.Sizeof(bucketHdr{})), uintptr(MAX_SLOTS)*(uintptr(1)+uintptr(t.keysize)+uintptr(t.valuesize)))
		// Update and then invalidate to point to nested ARRAY
		// arr.buckets[idx] = (*bucketHdr)(unsafe.Pointer(newArr))
		// println("len: ", len(arr.buckets), "parentIdx: ", data.parentIdx, ", idx: ", idx)
		sync_atomic_StorePointer((*unsafe.Pointer)(unsafe.Pointer(&arr.buckets[idx])), unsafe.Pointer(newArr))
		atomic.Storeuintptr(&hdr.lock, INVALID)

		// Now that we have converted the bucket successfully, we still haven't assigned nor found a spot for our current key-value.
		// In this case try again, to reduce contention and increase concurrency over the lock
		arr = newArr
		g.releaseBucket = nil

		goto next
	}

	// At this point, if firstEmpty == -1, then we exceeded the count without first finding one empty.
	// Hence, the first empty is going to be at idx hdr.count because there is none empty before it.
	// TODO: Clarification
	if firstEmpty == -1 {
		firstEmpty = int(count)
	}

	// At this point, firstEmpty is guaranteed to be non-zero and within bounds, hence we can safely assign to it
	data.assign(t, uint32(firstEmpty), hash, key, val)
	// Since we just assigned to a new empty slot, we need to increment count
	atomic.Store(&hdr.count, count+1)
	atomic.Xadd((*uint32)(unsafe.Pointer(&h.count)), 1)
}

/*
	Releases the current owned bucket. Should be noted that this is not a 'nosplit' function, and hence we can be
	preeempted... so, due to this, it is possible for anyone waiting on us to be running on the same OS Thread as
	we are (assuming we get preempted) and cause a deadlock... to remedy this, all spnning features some yielding
	to the scheduler.

	TODO: Make all spinning check if the current owner is not running?
*/
func maprelease() {
	g := getg()
	if g.releaseBucket != nil {
		// println("g #", g.goid, ": released lock")
		hdr := (*bucketHdr)(g.releaseBucket)

		atomic.Storeuintptr(&hdr.lock, UNLOCKED)

		g.releaseBucket = nil

	}
}

/*
	Searches for the requested key into the map, using the passed 'equal' callback to determine if it is equal or not.
	The reason for the 'equal' callback is to reduce data redundancy (in the sake of performance; a call to cmapaccess1_fast32
	will call cmapaccess2_fast32 which will call cmapaccess2 which will call this function...). This is because the test for
	equality depends on the actual type; a u32, u64, or string have faster variants, all of which can be called through this.

	If a bucket that the key hashes to is nil, it will return immediately.

	Syntax:

		value := map[key]
*/
func cmapaccess(t *maptype, h *hmap, key unsafe.Pointer, equal func(k1, k2 unsafe.Pointer) bool) (unsafe.Pointer, bool) {
	g := getg()

	// If we are currently interlocked on this map, take the fast path.
	for _, info := range g.interlockedData {
		if info.cmap == h.chdr {
			return cmapaccess_interlocked(t, info, h, key, equal)
		}
	}

	cmap := (*concurrentMap)(h.chdr)
	arr := &cmap.root
	var hash, idx, spins uintptr
	var backoff int64
	var hdr *bucketHdr
	gptr := uintptr(unsafe.Pointer(g))

	// Finds the bucket associated with the key's hash; if it is recursive we jump back to here.
next:
	// Obtain the hash, index, and bucketHdr
	hash = t.key.alg.hash(key, uintptr(arr.seed))
	idx = hash % uintptr(len(arr.buckets))
	hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))

	// Save time by looking ahead of time (testing) if the header or if the bucket is empty
	if hdr == nil || atomic.Load(&hdr.count) == 0 {
		return unsafe.Pointer(&DUMMY_RETVAL[0]), false
	}

	// Attempt to acquire lock
	for {
		// Reset backoff variables
		spins = 0
		backoff = DEFAULT_BACKOFF

		// Testing lock
		lock := atomic.Loaduintptr(&hdr.lock)

		// If the state of the bucket is INVALID, then either it's been deleted or been converted into an ARRAY; Reload and try again
		if lock == INVALID {
			// Reload hdr, since what it was pointed to has changed
			hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
			// If the hdr was deleted, then the data we're trying to find isn't here anymore (if it was at all)
			// hdr.count == 0 if another Goroutine has created a new bucketData after the old became INVALID. In this
			// case, there's still nothing here for us.
			if hdr == nil || atomic.Load(&hdr.count) == 0 {
				return unsafe.Pointer(&DUMMY_RETVAL[0]), false
			}
			// Loop again.
			continue
		}

		// If it's recursive, try again on new bucket
		if lock == ARRAY {
			arr = (*bucketArray)(unsafe.Pointer(hdr))
			goto next
		}

		// If we hold the lock
		if lock == gptr {
			throw("Reacquired lock on bucket we currently own: Failed invariant!")
		}

		// If the lock is uncontested
		if lock == UNLOCKED {
			// Attempt to acquire
			if atomic.Casuintptr(&hdr.lock, UNLOCKED, gptr) {
				g.releaseBucket = unsafe.Pointer(hdr)
				break
			}
			continue
		}

		// Keep track of the current lock-holder
		holder := lock & LOCKED_MASK

		// Tight-spin until the current lock-holder releases lock
		for {
			done := false
			for i := 0; i < GOSCHED_AFTER_SPINS; i, spins = i+1, spins+1 {
				// We test the lock on each iteration
				lock = atomic.Loaduintptr(&hdr.lock)
				// If the previous lock-holder released the lock, attempt to acquire again.
				if lock != holder {
					if spins > 100000 {
						println("...g # ", g.goid, ": Spins:", spins, ", Backoff:", backoff)
					}
					done = true
					break
				}

				procyield(uint32(MIN_SPIN_CYCLES))
			}
			if done {
				break
			}
			Gosched()

			if spins > 1000000 {
				println("...g # ", g.goid, ": Function: cmapassign, Crash Dump: {")
				println("holder: ", holder)
				println("lock: ", lock)
				println("gptr: ", gptr)
				println("hdr: {")
				println("\taddress: ", hdr)
				println("\tcount: ", hdr.count)
				println("\tlock: ", hdr.lock)
				println("\tparent: {")
				println("\t\taddress: ", hdr.parent)
				println("\t\tlock: ", hdr.parent.lock)
				println("\t\tcount: ", hdr.parent.count)
				println("\t\tlen: ", len((*bucketArray)(unsafe.Pointer(hdr.parent)).buckets))
				println("\t}")
				println("\tparentIdx: ", hdr.parentIdx)
				println("}")

				throw("Deadlock Detected!")
			}
		}
	}

	data := (*bucketData)(unsafe.Pointer(hdr))

	// Search the bucketData for the data needed
	for i, count := uint32(0), hdr.count; i < MAX_SLOTS && count > 0; i++ {
		currHash := data.tophash[i]
		tophash := uint8(hash >> HASH_SHIFT)
		// Top COULD be 0
		if tophash == 0 {
			tophash += 1
		}

		// We skip any empty hashes, but keep note of how many non-empty we find to know when to stop early
		if currHash == EMPTY {
			continue
		}
		count--

		// Check if the hashes are equal
		if currHash == tophash {
			otherKey := data.key(t, i)

			// Perform indirection on otherKey if necessary
			if t.indirectkey {
				otherKey = *(*unsafe.Pointer)(otherKey)
			}

			// If the keys are equal
			if equal(key, otherKey) {
				return data.value(t, i), true
			}
		}
	}

	// Only get to this point if we have not found the value in the map
	return unsafe.Pointer(&DUMMY_RETVAL[0]), false
}

/*
   Concurrent hashmap_fast.go function implementations. Called when you have a 4-byte key, such as a int32 or uint32, or struct with a size of 4-bytes.
*/
func cmapaccess1_fast32(t *maptype, h *hmap, key uint32) unsafe.Pointer {
	retval, _ := cmapaccess2_fast32(t, h, key)
	return retval
}

/*
   Concurrent hashmap_fast.go function implementations. Called when you have a 4-byte key, such as a int32 or uint32, or struct with a size of 4-bytes.
*/
func cmapaccess2_fast32(t *maptype, h *hmap, key uint32) (unsafe.Pointer, bool) {
	return cmapaccess(t, h, noescape(unsafe.Pointer(&key)),
		func(k1, k2 unsafe.Pointer) bool {
			return *(*uint32)(k1) == *(*uint32)(k2)
		})
}

/*
   Concurrent hashmap_fast.go function implementations. Called when you have a 8-byte key, such as a int64 or uint64, or struct with a size of 8-bytes.
*/
func cmapaccess1_fast64(t *maptype, h *hmap, key uint64) unsafe.Pointer {
	retval, _ := cmapaccess2_fast64(t, h, key)
	return retval
}

/*
   Concurrent hashmap_fast.go function implementations. Called when you have a 8-byte key, such as a int64 or uint64, or struct with a size of 8-bytes.
*/
func cmapaccess2_fast64(t *maptype, h *hmap, key uint64) (unsafe.Pointer, bool) {
	return cmapaccess(t, h, noescape(unsafe.Pointer(&key)),
		func(k1, k2 unsafe.Pointer) bool {
			return *(*uint64)(k1) == *(*uint64)(k2)
		})
}

/*
   Concurrent hashmap_fast.go function implementations. Called when you have a string key.
*/
func cmapaccess1_faststr(t *maptype, h *hmap, key string) unsafe.Pointer {
	retval, _ := cmapaccess2_faststr(t, h, key)
	return retval
}

/*
   Concurrent hashmap_fast.go function implementations. Called when you have a string key.
*/
func cmapaccess2_faststr(t *maptype, h *hmap, key string) (unsafe.Pointer, bool) {
	return cmapaccess(t, h, noescape(unsafe.Pointer(&key)),
		func(k1, k2 unsafe.Pointer) bool {
			sk1 := (*stringStruct)(k1)
			sk2 := (*stringStruct)(k2)
			return sk1.len == sk2.len &&
				(sk1.str == sk2.str || memequal(sk1.str, sk2.str, uintptr(sk1.len)))
		})
}

/*
	Syntax:

		value, present := map[key]
*/
func cmapaccess2(t *maptype, h *hmap, key unsafe.Pointer) (unsafe.Pointer, bool) {
	// g := getg().m.curg
	// println("g #", getg().goid, ": cmapaccess2!")

	return cmapaccess(t, h, key, t.key.alg.equal)
}

/*
	Syntax:

		value := map[key]
*/
func cmapaccess1(t *maptype, h *hmap, key unsafe.Pointer) unsafe.Pointer {
	// g := getg().m.curg
	println("g #", getg().goid, ": cmapaccess1")
	retval, _ := cmapaccess2(t, h, key)

	// Only difference is that we discard the boolean
	return retval
}

/*
	Removes the requested key from the map. If the bucket the key hashes to is nil, it will return immediately.
	If the element removed is the last element in the bucketData, it will atomically update it's parent reference
	with 'nil', consequently freeing the memory occupied by the now-empty bucket.

	Syntax:

		delete(map, key)
*/
func cmapdelete(t *maptype, h *hmap, key unsafe.Pointer) {
	// If we are currently interlocked on this map, take the fast path.
	g := getg()
	for _, info := range g.interlockedData {
		if info.cmap == h.chdr {
			cmapdelete_interlocked(t, info, h, key)
			return
		}
	}

	cmap := (*concurrentMap)(h.chdr)
	arr := &cmap.root
	var hash, idx, spins uintptr
	var backoff int64
	var hdr *bucketHdr
	gptr := uintptr(unsafe.Pointer(g))

	// Finds the bucket associated with the key's hash; if it is recursive we jump back to here.
next:
	// Obtain the hash, index, and bucketHdr.
	hash = t.key.alg.hash(key, uintptr(arr.seed))
	idx = hash % uintptr(len(arr.buckets))
	hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))

	// Save time by looking ahead of time (testing) if the header or if the bucket is empty
	if hdr == nil || atomic.Load(&hdr.count) == 0 {
		return
	}

	// Attempt to acquire lock
	for {
		// Reset backoff variables
		spins = 0
		backoff = DEFAULT_BACKOFF

		// Testing lock
		lock := atomic.Loaduintptr(&hdr.lock)

		// If the state of the bucket is INVALID, then either it's been deleted or been converted into an ARRAY; Reload and try again
		if lock == INVALID {
			// Reload hdr, since what it was pointed to has changed
			hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
			// If the hdr was deleted, then the data we're trying to find isn't here anymore (if it was at all)
			// hdr.count == 0 if another Goroutine has created a new bucketData after the old became INVALID. In this
			// case, there's still nothing here for us.
			if hdr == nil || atomic.Load(&hdr.count) == 0 {
				return
			}
			// Loop again.
			continue
		}

		// If it's recursive, try again on new bucket
		if lock == ARRAY {
			arr = (*bucketArray)(unsafe.Pointer(hdr))
			goto next
		}

		// If we hold the lock
		if lock == gptr {
			break
		}

		// If the lock is uncontested
		if lock == UNLOCKED {
			// Attempt to acquire
			if atomic.Casuintptr(&hdr.lock, UNLOCKED, gptr) {
				g.releaseBucket = unsafe.Pointer(hdr)
				break
			}
			continue
		}

		// Keep track of the current lock-holder
		holder := lock & LOCKED_MASK

		// Tight-spin until the current lock-holder releases lock
		for {
			done := false
			for i := 0; i < GOSCHED_AFTER_SPINS; i, spins = i+1, spins+1 {
				// We test the lock on each iteration
				lock = atomic.Loaduintptr(&hdr.lock)
				// If the previous lock-holder released the lock, attempt to acquire again.
				if lock != holder {
					if spins > 100000 {
						println("...g # ", g.goid, ": Spins:", spins, ", Backoff:", backoff)
					}
					done = true
					break
				}

				procyield(uint32(MIN_SPIN_CYCLES))
			}
			if done {
				break
			}
			Gosched()

			if spins > 1000000 {
				println("...g # ", g.goid, ": Function: cmapassign, Crash Dump: {")
				println("holder: ", holder)
				println("lock: ", lock)
				println("gptr: ", gptr)
				println("hdr: {")
				println("\taddress: ", hdr)
				println("\tcount: ", hdr.count)
				println("\tlock: ", hdr.lock)
				println("\tparent: {")
				println("\t\taddress: ", hdr.parent)
				println("\t\tlock: ", hdr.parent.lock)
				println("\t\tcount: ", hdr.parent.count)
				println("\t\tlen: ", len((*bucketArray)(unsafe.Pointer(hdr.parent)).buckets))
				println("\t}")
				println("\tparentIdx: ", hdr.parentIdx)
				println("}")

				throw("Deadlock Detected!")
			}
		}
	}

	data := (*bucketData)(unsafe.Pointer(hdr))
	count := atomic.Load(&hdr.count)

	// TODO: Document
	for i, cnt := uint32(0), count; i < MAX_SLOTS && cnt > 0; i++ {
		currHash := data.tophash[i]
		tophash := uint8(hash >> HASH_SHIFT)
		// Top COULD be 0
		if tophash == 0 {
			tophash += 1
		}

		// TODO: Document
		if currHash == EMPTY {
			continue
		}
		cnt--

		// If the hash matches, we can compare
		if currHash == tophash {
			otherKey := data.key(t, i)

			// Perform indirection on otherKey if necessary
			if t.indirectkey {
				otherKey = *(*unsafe.Pointer)(otherKey)
			}

			// If they match, we are set to remove them from the bucket
			if t.key.alg.equal(key, otherKey) {
				memclr(data.key(t, i), uintptr(t.keysize))
				memclr(data.value(t, i), uintptr(t.valuesize))
				data.tophash[i] = EMPTY
				atomic.Xadd((*uint32)(unsafe.Pointer(&h.count)), -1)
				atomic.Xadd(&hdr.count, -1)
				break
			}
		}
	}

	// If this bucketData is empty, we release it, making it eaiser for the iterator to determine it is empty.
	// For emphasis: If we removed the last element, we delete the bucketData.
	if count == 0 {
		sync_atomic_StorePointer((*unsafe.Pointer)(unsafe.Pointer(&arr.buckets[idx])), nil)
		// arr.buckets[idx] = nil
		atomic.Storeuintptr(&hdr.lock, INVALID)
		g.releaseBucket = nil

		// Also decrement number of buckets
		atomic.Xadd(&arr.count, -1)
	}
}

///////////////////////////////////////////
//			SYNC.INTERLOCKED			//
/////////////////////////////////////////

/*
	Acquires the value corresponding to the 'key' passed in the map. If the key does not exist, it will automatically allocate space
	for the user, performing any duties that 'insert' would when needing to resize. Note that, even if the key/value is allocated, it
	is marked as KEY_DELETED, and so attempts to determine the presence of the key is accurate: if it wasn't originally there, it will
	still return 'false'. As well, if the user deletes a key while it is acquired, it will only zero the value, but keep the key around
	so that the bucket we have remains in a consistent state.

	This runtime function is forwarded to the 'reflect' package, which is capable of generating the runtime information needed (such as
	runtime-type and map header), which then has it's own function to forward to the 'sync' package, making 'sync.Interlocked' a possibility.
*/
//go:linkname mapacquire reflect.interlockedImpl
func mapacquire(t *maptype, h *hmap, key unsafe.Pointer) {
	if h.chdr == nil {
		throw("sync.Interlocked invoked on a non-concurrent map!")
	}

	info := (*interlockedInfo)(newobject(t.interlockedinfo))
	info.cmap = h.chdr
	cmap := (*concurrentMap)(h.chdr)
	arr := &cmap.root
	var hash, idx, spins uintptr
	var backoff int64
	var hdr *bucketHdr
	g := getg()
	gptr := uintptr(unsafe.Pointer(g))

	// If we already have this key interlocked we are done.
	for _, data := range g.interlockedData {
		if data.cmap == h.chdr {
			throw("Attempt to acquire a key while currently interlocked!")
		}
	}

	g.interlockedData = append(g.interlockedData, info)

	// Finds the bucket associated with the key's hash; if it is recursive we jump back to here.
next:
	// Obtain the hash, index, and bucketHdr.
	hash = t.key.alg.hash(key, uintptr(arr.seed))
	idx = hash % uintptr(len(arr.buckets))
	hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))

	// Tests validity of header. Jumped to when we do not need to repeatedly rehash the key.
test:
	// If hdr is nil, then no bucketData has been created yet. Do a wait-free creation of bucketData;
	for hdr == nil {
		// Note that bucketData has the same first 3 fields as bucketHdr, and can be safely casted
		newHdr := (*bucketHdr)(newobject(t.bucketdata))
		// Since we're setting it, may as well attempt to acquire lock
		newHdr.lock = gptr
		newHdr.parent = arr
		newHdr.parentIdx = uint32(idx)
		newHdr.count = uint32(1)
		// If we fail, then some other Goroutine has already placed theirs.
		if atomic.Casp1((*unsafe.Pointer)(unsafe.Pointer(&arr.buckets[idx])), nil, unsafe.Pointer(newHdr)) {
			// If we succeed, then we own this bucket and need to keep track of it
			info.hdr = newHdr
			// Also increment count of buckets
			atomic.Xadd(&arr.count, 1)
			atomic.Xadd((*uint32)(unsafe.Pointer(&h.count)), 1)

			// Since we just created this bucket, we can easily assign to the first slot.
			data := (*bucketData)(unsafe.Pointer(newHdr))
			data.assign(t, uint32(0), hash, key, nil)

			info.key = data.key(t, uint32(0))
			info.value = data.value(t, uint32(0))
			info.hash = &data.tophash[0]

			// Mark as being not-present so it will be cleaned up if the user never assigns to it.
			info.flags |= KEY_DELETED

			return
		}
		// Reload hdr
		hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
	}

	// Attempt to acquire lock
	for {
		// Reset backoff variables
		spins = 0
		backoff = DEFAULT_BACKOFF

		// Testing lock
		lock := atomic.Loaduintptr(&hdr.lock)

		// If the state of the bucket is INVALID, then either it's been deleted or been converted into an ARRAY; Reload and try again
		if lock == INVALID {
			// Reload hdr, since what it was pointed to has changed
			hdr = (*bucketHdr)(atomic.Loadp(unsafe.Pointer(&arr.buckets[idx])))
			// Test hdr again; skip rehashing
			goto test
		}

		// If it's recursive, try again on new bucket
		if lock == ARRAY {
			arr = (*bucketArray)(unsafe.Pointer(hdr))
			goto next
		}

		// If we hold the lock
		if lock == gptr {
			break
		}

		// If the lock is uncontested
		if lock == UNLOCKED {
			// Attempt to acquire
			if atomic.Casuintptr(&hdr.lock, UNLOCKED, gptr) {
				info.hdr = hdr
				break
			}
			continue
		}

		// Keep track of the current lock-holder
		holder := lock & LOCKED_MASK

		// Tight-spin until the current lock-holder releases lock
		for {
			done := false
			for i := 0; i < GOSCHED_AFTER_SPINS; i, spins = i+1, spins+1 {
				// We test the lock on each iteration
				lock = atomic.Loaduintptr(&hdr.lock)
				// If the previous lock-holder released the lock, attempt to acquire again.
				if lock != holder {
					if spins > 100000 {
						println("...g # ", g.goid, ": Spins:", spins, ", Backoff:", backoff)
					}
					done = true
					break
				}

				procyield(uint32(MIN_SPIN_CYCLES))
			}
			if done {
				break
			}
			Gosched()

			if spins > 1000000 {
				println("...g # ", g.goid, ": Function: cmapassign, Crash Dump: {")
				println("holder: ", holder)
				println("lock: ", lock)
				println("gptr: ", gptr)
				println("hdr: {")
				println("\taddress: ", hdr)
				println("\tcount: ", hdr.count)
				println("\tlock: ", hdr.lock)
				println("\tparent: {")
				println("\t\taddress: ", hdr.parent)
				println("\t\tlock: ", hdr.parent.lock)
				println("\t\tcount: ", hdr.parent.count)
				println("\t\tlen: ", len((*bucketArray)(unsafe.Pointer(hdr.parent)).buckets))
				println("\t}")
				println("\tparentIdx: ", hdr.parentIdx)
				println("}")

				throw("Deadlock Detected!")
			}
		}
	}

	info.hdr = hdr

	data := (*bucketData)(unsafe.Pointer(hdr))
	firstEmpty := -1

	// If we do not find the requested key to interlock, we must create a new key for the user... same song and dance
	// as cmapassign....
	// Otherwise, we must scan all hashes to find a matching hash; if they match, check if they are equal
	for i, count := uint32(0), hdr.count; i < MAX_SLOTS && count > 0; i++ {
		currHash := data.tophash[i]
		tophash := uint8(hash >> HASH_SHIFT)
		// Top COULD be 0
		if tophash == 0 {
			tophash += 1
		}

		if currHash == EMPTY {
			// Keep track of the first empty so we know what to assign into if we do not find a match
			if firstEmpty == -1 {
				firstEmpty = int(i)
			}
			continue
		}
		count--

		// If the hash matches, check to see if keys are equal
		if tophash == currHash {
			otherKey := data.key(t, i)

			// If they are equal, keep track of the respective key, value and hash.
			if t.key.alg.equal(key, otherKey) {
				info.key = data.key(t, i)
				info.value = data.value(t, i)
				info.hash = &data.tophash[i]

				return
			}
		}
	}

	// If firstEmpty is still -1 and the bucket is full, that means we did not find any empty slots, and should convert immediate
	if firstEmpty == -1 && hdr.count == MAX_SLOTS {
		// println("g #", getg().goid, ": Resizing...")
		// Allocate and initialize
		newArr := (*bucketArray)(newobject(t.bucketarray))
		newArr.lock = ARRAY
		newArr.buckets = make([]*bucketHdr, len(arr.buckets)*2)
		newArr.seed = fastrand1()
		newArr.parent = arr
		newArr.parentIdx = uint32(idx)

		// Rehash and move all key-value pairs
		for i := uint32(0); i < uint32(MAX_SLOTS); i++ {
			k := data.key(t, i)
			v := data.value(t, i)

			// Rehash the key to the new seed
			newHash := t.key.alg.hash(k, uintptr(newArr.seed))
			newIdx := newHash % uintptr(len(newArr.buckets))
			newHdr := newArr.buckets[newIdx]
			newData := (*bucketData)(unsafe.Pointer(newHdr))

			// Check if the bucket is nil, meaning we haven't allocated to it yet.
			if newData == nil {
				newArr.buckets[newIdx] = (*bucketHdr)(newobject(t.bucketdata))
				newArr.count++

				newData = (*bucketData)(unsafe.Pointer(newArr.buckets[newIdx]))
				newData.parent = newArr
				newData.parentIdx = uint32(newIdx)
				newData.assign(t, 0, newHash, k, v)
				newData.count++

				continue
			}

			// If it is not nil, then we must scan for the first non-empty slot
			for j := uint32(0); j < uint32(MAX_SLOTS); j++ {
				currHash := newData.tophash[j]
				if currHash == EMPTY {
					newData.assign(t, j, newHash, k, v)
					newData.count++
					break
				}
			}
		}

		// Now dispose of old data and update the header's bucket; We have to be careful to NOT overwrite the header portion
		memclr(add(unsafe.Pointer(data), unsafe.Sizeof(bucketHdr{})), uintptr(MAX_SLOTS)*(uintptr(1)+uintptr(t.keysize)+uintptr(t.valuesize)))
		// Update and then invalidate to point to nested ARRAY
		// arr.buckets[idx] = (*bucketHdr)(unsafe.Pointer(newArr))
		sync_atomic_StorePointer((*unsafe.Pointer)(unsafe.Pointer(&arr.buckets[idx])), unsafe.Pointer(newArr))
		atomic.Storeuintptr(&hdr.lock, INVALID)

		// Now that we have converted the bucket successfully, we still haven't assigned nor found a spot for our current key-value.
		// In this case try again, to reduce contention and increase concurrency over the lock
		arr = newArr
		info.hdr = nil

		goto next
	}

	// At this point, if firstEmpty == -1, then we exceeded the count without first finding one empty.
	// Hence, the first empty is going to be at idx hdr.count because there is none empty before it.
	// TODO: Clarification
	if firstEmpty == -1 {
		firstEmpty = int(hdr.count)
	}
	// TODO: What if t.indirectvalue is true? Correct this!
	// At this point, firstEmpty is guaranteed to be non-zero and within bounds, hence we can safely assign to it
	data.assign(t, uint32(firstEmpty), hash, key, unsafe.Pointer(&DUMMY_RETVAL[0]))
	info.key = data.key(t, uint32(firstEmpty))
	info.value = data.value(t, uint32(firstEmpty))
	info.hash = &data.tophash[uint32(firstEmpty)]
	info.flags |= KEY_DELETED
	// Since we just assigned to a new empty slot, we need to increment count
	atomic.Xadd(&hdr.count, 1)
	atomic.Xadd((*uint32)(unsafe.Pointer(&h.count)), 1)
}

/*
	Releases the current interlocked bucket. If the key has been marked for deletion, it will ensure that it zero's
	the actual key and hash as well (as during interlocked deletion, it only zero's the value). As well, it handles
	'cmapdelete' duty of deleting the last element if needed.
*/
//go:linkname maprelease_interlocked reflect.interlockedReleaseImpl
func maprelease_interlocked(t *maptype, h *hmap) {
	var info *interlockedInfo
	g := getg()
	for _, data := range g.interlockedData {
		if data.cmap == h.chdr {
			info = data
			break
		}
	}

	// Handle cases where the current key has been marked for deletion. At this point, the corresponding value has already been zero'd.
	// Note, for iteration it must handle deleting its own keys after each successful iteration.
	if (info.flags & KEY_DELETED) != 0 {
		memclr(unsafe.Pointer(info.key), uintptr(t.keysize))
		*info.hash = EMPTY
		atomic.Xadd(&info.hdr.count, -1)
	}

	// Check for the case when we deleted all elements in this bucket, and if we did, invalidate and delete it
	if info.hdr.count == 0 {
		// Invalidate and release the bucket (as it is being deleted)
		sync_atomic_StorePointer((*unsafe.Pointer)(unsafe.Pointer(&info.hdr.parent.buckets[info.hdr.parentIdx])), nil)
		atomic.Storeuintptr(&info.hdr.lock, INVALID)

		// Also decrement number of buckets
		atomic.Xadd(&info.hdr.parent.count, -1)
	} else {
		// Otherwise, just release the lock on the bucket
		atomic.Storeuintptr(&info.hdr.lock, UNLOCKED)
	}

	interlocked_release(h)
}

///////////////////////////////////
//			FAST PATH			//
/////////////////////////////////

/*
	Fast-Path access of a cached element.
*/
func cmapaccess_interlocked(t *maptype, info *interlockedInfo, h *hmap, key unsafe.Pointer, equal func(k1, k2 unsafe.Pointer) bool) (unsafe.Pointer, bool) {
	// Do not allow accesses with keys not currently owned.
	if !equal(info.key, key) {
		throw("Key indexed must be same as interlocked key!")
	}

	// If the key had been 'deleted', return the zero'd portion
	if (info.flags & KEY_DELETED) != 0 {
		return unsafe.Pointer(&DUMMY_RETVAL[0]), false
	}

	// Otherwise return the value directly
	return info.value, true
}

func cmapaccess2_interlocked(t *maptype, info *interlockedInfo, h *hmap, key unsafe.Pointer) (unsafe.Pointer, bool) {
	return cmapaccess_interlocked(t, info, h, key, t.key.alg.equal)
}

func cmapaccess1_interlocked(t *maptype, info *interlockedInfo, h *hmap, key unsafe.Pointer) unsafe.Pointer {
	retval, _ := cmapaccess2_interlocked(t, info, h, key)

	// Only difference is that we discard the boolean
	return retval
}

func cmapaccess1_fast32_interlocked(t *maptype, info *interlockedInfo, h *hmap, key uint32) unsafe.Pointer {
	retval, _ := cmapaccess2_fast32_interlocked(t, info, h, key)
	return retval
}

func cmapaccess2_fast32_interlocked(t *maptype, info *interlockedInfo, h *hmap, key uint32) (unsafe.Pointer, bool) {
	return cmapaccess_interlocked(t, info, h, noescape(unsafe.Pointer(&key)),
		func(k1, k2 unsafe.Pointer) bool {
			return *(*uint32)(k1) == *(*uint32)(k2)
		})
}

func cmapaccess1_fast64_interlocked(t *maptype, info *interlockedInfo, h *hmap, key uint64) unsafe.Pointer {
	retval, _ := cmapaccess2_fast64_interlocked(t, info, h, key)
	return retval
}

func cmapaccess2_fast64_interlocked(t *maptype, info *interlockedInfo, h *hmap, key uint64) (unsafe.Pointer, bool) {
	return cmapaccess_interlocked(t, info, h, noescape(unsafe.Pointer(&key)),
		func(k1, k2 unsafe.Pointer) bool {
			return *(*uint64)(k1) == *(*uint64)(k2)
		})
}

func cmapaccess1_faststr_interlocked(t *maptype, info *interlockedInfo, h *hmap, key string) unsafe.Pointer {
	retval, _ := cmapaccess2_faststr_interlocked(t, info, h, key)
	return retval
}

func cmapaccess2_faststr_interlocked(t *maptype, info *interlockedInfo, h *hmap, key string) (unsafe.Pointer, bool) {
	return cmapaccess_interlocked(t, info, h, noescape(unsafe.Pointer(&key)),
		func(k1, k2 unsafe.Pointer) bool {
			sk1 := (*stringStruct)(k1)
			sk2 := (*stringStruct)(k2)
			return sk1.len == sk2.len &&
				(sk1.str == sk2.str || memequal(sk1.str, sk2.str, uintptr(sk1.len)))
		})
}

/*
	Fast Path to assigning to the value slot.
*/
func cmapassign_interlocked(t *maptype, info *interlockedInfo, h *hmap, key, value unsafe.Pointer) {
	k := info.key
	v := info.value

	// Perform some indirection on key if necessary; necessary for test of equality
	if t.indirectkey {
		k = *(*unsafe.Pointer)(k)
	}

	// Do not allow accesses with keys not currently owned.
	if !t.key.alg.equal(k, key) {
		throw("Key indexed must be same as interlocked key!")
	}

	// Since we will be doing a memcpy regardless of if it is present or not, unset the KEY_DELETED bit, but keep track of old value
	present := (info.flags & KEY_DELETED) == 0
	if !present {
		info.flags &= ^KEY_DELETED
	}

	// In the case that the key was present, we are updating, hence we need to check if we also need to update the key as well
	if present {
		// Perform some indirection if necessary
		if t.indirectvalue {
			v = *(*unsafe.Pointer)(v)
		}

		// If we are required to update key, do so
		if t.needkeyupdate {
			typedmemmove(t.key, k, key)
		}

		typedmemmove(t.elem, v, value)
	} else {
		// In the case it has been deleted and we assigning again, we do a direct assignment
		// Perform indirection needed
		if t.indirectvalue {
			vmem := newobject(t.elem)
			*(*unsafe.Pointer)(v) = vmem
			v = vmem
		}

		typedmemmove(t.elem, v, value)

		// Increment since we decrement when we originally delete it.
		atomic.Xadd(&info.hdr.count, 1)
		atomic.Xadd((*uint32)(unsafe.Pointer(&h.count)), 1)
	}
}

/*
	Fast Path to marking the current key-value as deleted (only zero's the value)
*/
func cmapdelete_interlocked(t *maptype, info *interlockedInfo, h *hmap, key unsafe.Pointer) {
	// println("g #", getg().goid, ": cmapdelete_interlocked")
	// Do not allow accesses with keys not currently owned.
	if !t.key.alg.equal(info.key, key) {
		throw("Key indexed must be same as interlocked key!")
	}

	// If we do not hold the lock we messed up...
	if info.hdr.lock != uintptr(unsafe.Pointer(getg())) {
		throw("Interlocked function, but hdr owned is not ours!")
	}

	// If the key had already been 'deleted' we're done.
	if (info.flags & KEY_DELETED) != 0 {
		return
	}

	// Update flags and zero value
	info.flags |= KEY_DELETED
	memclr(info.value, uintptr(t.valuesize))
	atomic.Xadd((*uint32)(unsafe.Pointer(&h.count)), -1)
	atomic.Xadd(&info.hdr.count, -1)
}
