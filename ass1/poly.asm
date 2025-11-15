poly	START 0
. koda	
	LDA x
	LDT x
	LDS ena
	MULR S, T
	MULR A, T
	MULR A, T
	MULR A, T
	ADDR T, B
	STT prvi
	
	LDT x
	LDS dva
	MULR S, T
	MULR A, T
	MULR A, T
	ADDR T, B
	STT drugi
	
	LDT x
	LDS tri
	MULR S, T
	MULR A, T
	ADDR T, B
	STT tretji
	
	LDT x
	LDS stiri
	MULR S, T
	ADDR T, B
	STT cetrti
	
	LDS pet
	ADDR S, B
	STS peti
	
	STB poli

halt	J halt
	
. podatki 
x	WORD 2
ena	WORD 1
dva	WORD 2
tri	WORD 3
stiri	WORD 4
pet	WORD 5
prvi	RESW 1
drugi	RESW 1
tretji 	RESW 1
cetrti	RESW 1
peti	RESW 1
poli	RESW 1

	END poly
	
