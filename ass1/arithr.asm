arithr	START 0
. koda	
	LDA x
	LDB y
	
	RMO A, S
	RMO B, T
	ADDR T, S
	STS sum
	
	RMO A, S
	RMO B, T
	SUBR T, S
	STS diff
	
	RMO A, S
	RMO B, T
	MULR T, S
	STS prod
	
	RMO A, S
	RMO B, T
	DIVR T, S
	STS quot
	
	RMO A, S
	RMO B, T
	DIVR T, S
	MULR T, S
	RMO A, T
	SUBR S, T
	STT mod
	
halt	J halt
	
. podatki 
x	WORD 17
y	WORD 5
sum	RESW 1
diff	RESW 1
prod	RESW 1
quot	RESW 1
mod	RESW 1

	END arithr
	
