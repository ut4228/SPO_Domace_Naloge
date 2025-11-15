cat     START 0

loop	CLEAR A
	+RD #0x1A
	COMP ZERO
	JEQ halt
	WD #1
	J loop
	
halt 	J halt

ZERO	WORD 0

        END cat

