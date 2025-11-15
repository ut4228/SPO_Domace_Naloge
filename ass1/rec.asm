rec     START 0

	+JSUB sinit

readN   CLEAR A
        RD #0
        COMP ZERO           . primerjaj z '0' (ASCII 48)
        JEQ halt

        SUB #48            . ASCII -> integer v A (0..9)

        +JSUB fac            . kličemo rekurzivno fakulteto (argument v A, rezultat v A)
        JSUB  printDec       . izpiši A kot decimalno število

        LDA #10
        WD #1

        J readN

halt    J halt

. ----- izpis A v decimalni obliki -----
printDec COMP  #0
	 STA   saveA
         JGT   pd_loop_prep
         RSUB

pd_loop_prep
         STA   pd_orig       . shrani originalno A
         DIV   TEN           . A = n/10 ; X = n%10 (ignoriramo X, sledimo tvoji logiki)
         STA   pd_quot       . quotient
         LDA   pd_quot
         MUL   TEN           . A = quotient*10
         STA   pd_q10
         LDA   pd_orig
         SUB   pd_q10       . A = remainder
         
         STA @stkp
         JSUB  spush         . push cifra
         
         LDA   pd_quot       . A = quotient
         COMP  #0
         JGT   pd_loop_prep  . nadaljuj dokler quotient > 0

. zdaj na skladu cifre v obratnem vrstnem redu => pop & izpis
pd_out  JSUB  spop
	LDA   @stkp
	ADD   #48
        WD    #1
        
        LDA   saveA
        DIV   TEN
        STA   saveA
        COMP  #0
        JGT   pd_out
        J     readN
fac	STL @stkp	. shrani vrednosti na sklad
	JSUB spush
	
	STB @stkp
	JSUB spush
	
	COMP #2		. base case
	JLT facEx
	RMO A, B
	SUB #1		. recursive case
	JSUB fac
	MULR B, A
	
facEx	JSUB spop	. nalozi prejsne vrednosti
	LDB @stkp
	
	JSUB spop
	LDL @stkp
	
	RSUB
	
. nastavi stkp na zacetek sklada
sinit	LDA #stk
	STA stkp
	RSUB
	
. poveca stkp za dolzino besede (3)
spush	STA stkA
	LDA stkp
	ADD #3
	STA stkp
	LDA stkA
	RSUB
	
. zmanjsa stkp za dolzino besede (3)
spop	STA stkA
	LDA stkp
	SUB #3
	STA stkp
	LDA stkA
	RSUB	
	
stkp	WORD 0
stkA	WORD 0
stk	RESW 1000

TEN     WORD  10
pd_orig     WORD  0
pd_quot   WORD  0
pd_q10    WORD  0
saveA    WORD  0

ZERO	WORD 0
STDIN	BYTE X'0A'

        END rec
