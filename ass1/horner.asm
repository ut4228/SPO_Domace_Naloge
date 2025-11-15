horner  START 0
. koda	
        LDA ena
        LDX x

        MULR X, A
        ADD dva

        MULR X, A
        ADD tri

        MULR X, A
        ADD stiri

        MULR X, A
        ADD pet

        STA result
        
halt	J halt

. podatki 
x       WORD 2
ena	WORD 1
dva	WORD 2
tri	WORD 3
stiri	WORD 4
pet	WORD 5
result  RESW 1

	END horner
