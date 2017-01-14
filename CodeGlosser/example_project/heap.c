#include <include/mm/heap.h>
#include <include/helpers.h>
#include <string.h>

// Idenitifies that the bitmap's entry is free to use
static const uint8_t FREE = 0;

void memheap_init(memheap_t *heap) {
	heap->head = NULL;
}

// The bitmap of a superblock is located directly after the superblock itself.
// The number of entries is equivalent to the number of blocks.
static inline uint8_t *memblock_get_bitmap(memblock_t *sblock) {
	return (uint8_t *) &sblock[1];
}

// Obtains the number of blocks in this superblock.
static inline uint32_t memblock_get_block_count(memblock_t *sblock) {
	return sblock->total_size / sblock->block_size;
}

// Generates an identifier that is distinct from the two passed identifier
static uint8_t generate_identifier(uint8_t x, uint8_t y) {
	uint8_t z = 1;
	for(; z == x || z == y; ++z);
	return z;
}

void memheap_add_block(memheap_t *heap, uintptr_t addr, uint32_t size, uint32_t block_size) {
	// We reserve the first bytes of memory (pointed to by 'addr') for the superblock itself. Since the superblock
	// takes up space, we must make the appropriate corrections to the size.
	memblock_t *sblock = (memblock_t *) addr;
	sblock->total_size = size - sizeof(memblock_t);
	sblock->block_size = block_size;

	// Make this the head of the heap's list of blocks
	sblock->next = heap->head;
	heap->head = sblock;

	uint32_t block_count = sblock->total_size / sblock->block_size;
	uint8_t *bitmap = memblock_get_bitmap(sblock);

	// Clear our bitmap
	memset(bitmap, FREE, block_count);
	
	// Reserve room for bitmap
	block_count = CEILING(block_count, block_size);
	memset(bitmap, !FREE, block_count);

	// We used some space for the bitmap, so we need to keep track of that.
	sblock->used = block_count;
}

void *memheap_alloc(memheap_t *heap, uint32_t size) {
	// For each superblock...
	for (memblock_t *sblock = heap->head; sblock; sblock = sblock->next) {
		// If this superblock is large enough
		if (size <= (sblock->total_size - (sblock->used * sblock->block_size))) {
			// Calculate the information needed for this superblock to fulfill our request
			uint32_t block_count = memblock_get_block_count(sblock);
			uint32_t blocks_needed = CEILING(size, sblock->block_size);
			uint8_t *bitmap = memblock_get_bitmap(sblock);

			// For each bitmap entry...
			for (uint32_t i = 0; i < block_count; i++) {
				// Try to find enough contiguous blocks that can satisfy this request
				if (bitmap[i] == FREE) {
					// Determine if there is enough free blocks at this offset. There is enough if the following conditions are met...
					// 1) The superblock has potentially enough blocks to satisfy the request (Confirmed true if we got this far...)
					// 2) All contiguous entries are free, as we need them to be to merge them for the user's request
					// 3) The current block is in range and not out of bounds ((n < blocks_needed) && (i + n) < block_count)
					uint32_t n = 0;
					for (; bitmap[i + n] == FREE && n < blocks_needed && (i + n) < block_count; n++);

					// At this point we know we have enough blocks to fit the allocation
					if (n == blocks_needed) {
						// Need an ID that can differentiate between this and other allocated blocks
						// For this to be true, we need to find one distinct from our previous and next blocks (if applicable)
						// Note: 'i' cannot be 0 as the first few blocks are reserved for the bitmap when the superblock is added.
						uint8_t id = generate_identifier(bitmap[i - 1], bitmap[i + n]);

						// Declare blocks as in use
						memset(&bitmap[i], id, n);

						// Update count
						sblock->used += n;

						return (void *) (i * sblock->block_size + (uintptr_t) &sblock[1]);
					}

					// This chunk of memory is not in use, skip over it. i gets incremented in next iteration.
					i += (n - 1);
					continue;
				}
			}
		}
	}

	// No memory found...
	return NULL;
}

void memheap_free(memheap_t *heap, void *ptr) {
	// For each superblock...
	for (memblock_t *sblock = heap->head; sblock; sblock = sblock->next) {
		// If the pointer is within the superblock
		if ((uintptr_t) ptr > (uintptr_t) sblock && (uintptr_t) ptr < (uintptr_t) sblock + sizeof(memblock_t) + sblock->total_size) {
			uintptr_t block_start = ((uintptr_t) ptr - (uintptr_t) &sblock[1]) / sblock->block_size;
			uint8_t *bitmap = memblock_get_bitmap(sblock);

			// All blocks for the same allocation have the same ID, this was enforced in allocation. Find these and deallocate them.
			uint8_t id = bitmap[block_start];
			uint32_t block_count = memblock_get_block_count(sblock);
			uintptr_t block_offset = block_start;
			for (; bitmap[block_offset] == id && block_offset < block_count; block_offset++) {
				bitmap[block_offset] = 0;
			}

			// Update used counter
			sblock->used -= block_offset - block_start;
		}
	}
}