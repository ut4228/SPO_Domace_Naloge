arith	START 0
. koda	
	LDA x
	LDB y
	ADDR B, A
	STA sum
	
	LDA x
	LDB y
	SUBR B, A
	STA diff
	
	LDA x
	LDB y
	MULR B, A
	STA prod
	
	LDA x
	LDB y
	DIVR B, A
	STA quot
	
	LDA x
	LDB y
	DIVR B, A
	MULR A, B
	LDA x
	SUBR B, A
	STA mod
	
halt	J halt
	
. podatki 
x	WORD 17
y	WORD 5
sum	RESW 1
diff	RESW 1
prod	RESW 1
quot	RESW 1
mod	RESW 1

	END arith
	
