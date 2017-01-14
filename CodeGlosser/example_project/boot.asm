; Constant Declarations for Multiboot Header (GRUB)
ALIGN_ON_PAGE equ 1 << 0 ; Align modules on page boundaries
USE_MMAP equ 1 << 1 ; Provide memory map
FLAGS equ ALIGN_ON_PAGE | USE_MMAP ; Multiboot flag
MAGIC equ 0x1BADB002 ; 'Magic Number' needed to allow bootloader to find header
CHECKSUM equ -(MAGIC + FLAGS) ; Checksum to prove we are multiboot

; Declare the multiboot header needed by GRUB
section .multiboot

	align 4
		dd MAGIC
		dd FLAGS
		dd CHECKSUM

; Setup stack pointer register (esp), as its contents are currently undefined. Allocate 16KBs
section .create_stack nobits

	align 4
		stack_bottom:
			resb 16 * 1024
		stack_top:

; Below we setup paging (virtual memory) and move our kernel to the higher half
VIRTUAL_ADDRESS_START equ 0xC0000000
; Since each index in the Page Directory only uses the higher 12 bits, which is
; used as it's index, we keep a convenient constant of it.
KERNEL_INDEX equ (VIRTUAL_ADDRESS_START >> 22)
; A constant for Page Directory entries that sets the PS, RW, and P bits, which specify
; that the pages should be 4MBs in size, that it can be read and written to, and is present
; respectively.
PDE_DEFAULT equ 0x00000083

; Setup the paging structures needed to bootstrap our kernel
section .data
	align 0x1000

	; This is the Page Directory that we use to bootstrap.
	bootstrap_page_directory:
		; Because we are remapping our kernel to KERNEL_START, our current
		; instruction pointer will cause us to page fault (and then double fault and
		; then triple fault) and trigger a hardware reset. Hence before we enable paging
		; we must identity map each virtual address to it's respective physical address.
		dd PDE_DEFAULT
		; All pages besides the kernel's are not present in memory
		times (KERNEL_INDEX - 1) dd 0
		; Kernel Entry
		dd PDE_DEFAULT
		; Pages after the kernel
		times (1024 - KERNEL_INDEX - 1) dd 0

section .text

; This is the entry point for the linker, which will start the instruction pointer in a
; valid physical memory location.
global start
	start equ (_start - VIRTUAL_ADDRESS_START)

global _start
	
	; Setup paging -- Note that at this point, we are still in physical memory, and as such we need
	; to ensure that we begin relative to the physical location 0x0.
	_start:
		; Load the Page Directory we setup above
		mov ecx, (bootstrap_page_directory - VIRTUAL_ADDRESS_START)
		mov cr3, ecx

		; Enable 4MB paging by setting the PSE bit in CR4
		mov ecx, cr4
		or ecx, 0x00000010
		mov cr4, ecx

		; Enable paging by setting the PG bit in CR4
		mov ecx, cr0
		or ecx, 0x80000000
		mov cr0, ecx

		; NOTE: At this point in time, paging has been enabled, and we are now using virtual memory.
		; This includes the instruction pointer, and as such we need to perform a long jump. As well,
		; the CPU needs to clear it's instruction cache it prefetched, which can be cleaned out by
		; performing a branch instruction, so this does performs double duty.
		lea ecx, [__start]
		jmp ecx

	; Setting up the stack and finally passing control back to our preferred language (C)
	__start:
		; Unmap the identity mapping established above of 0 - 4MBs, as we no longer require it.
		; At this point, our instruction pointer no points into 0xC0000000+ area.
		mov dword [bootstrap_page_directory], 0
		invlpg [0]

		; Setup the stack pointer to point to the stack allocated above
		mov esp, stack_top

		; EBX contains a pointer to the multiboot info structure that we should save for later
		; Since it is the physical address, we need to convert it to it's virtual address
		add ebx, VIRTUAL_ADDRESS_START
		push ebx

		; Zero EBP which is used to signify the end of a stack trace
		mov ebp, 0

		; Initialize the core facilities of the kernel
		extern kernel_init
		call kernel_init

		; Clean up stack frame
		add esp, 4

		; Zero EBP again since kernel_init will have changed it
		mov ebp, 0

		; Finally, jump to kmain, and leave assembly behind for good
		extern kernel_main
		call kernel_main

		; If we do end up back here, the bootloader is gone and hence there is nothing left to do but halt permanently
		cli
	.hang:
		hlt
		jmp .hang


; gdt_flush -- Load the GDT (Global Descriptor Table)
global gdt_flush

	gdt_flush:
		; Obtain the gdt_ptr passed and load it
		mov eax, [esp + 4]
		lgdt [eax]

		; Setup the kernel's Data Segment Descriptor
		mov ax, 0x10
		mov ds, ax
		mov es, ax
		mov fs, ax
		mov gs, ax
		mov ss, ax

		; Far Jump to the kernel's Code Segment flush label
		jmp 0x8:.flush
	.flush:
		ret

; idt_flush -- Load the IDT (Interrupt Descriptor Table)
global idt_flush

	idt_flush:
		; Obtain idt_ptr structure on stack and load it.
		mov eax, [esp + 4]
		lidt [eax]
		ret