guess   START 0

        LDA #3
        STA seed
        
newSecret
        JSUB random   	 . A = random (0..9)
        STA secret

gameLoop
        RD #0
        STA tmpChar

        LDA tmpChar
        COMP #10       	. LF?
        JEQ gameLoop  	. prezri newline
        
        LDA tmpChar
        SUB #48       	. pretvori ASCII v 0..9
        STA userNum

        LDA userNum
        COMP secret
        JEQ correct
        JLT tooLow
        JGT tooHigh
       
halt    J halt

tooHigh CLEAR   X
loop1   LDCH msgHigh, X
        WD #1
        TIX #msgHlen
        JLT loop1

        J gameLoop

tooLow  CLEAR X
loop2   LDCH msgLow, X
        WD #1
        TIX #msgLlen
        JLT loop2

        J gameLoop

correct CLEAR X
loop3   LDCH msgWin, X
        WD #1
        TIX #msgWlen
        JLT loop3

        J newSecret

. f: X = (seed + 7) mod 10
random
        LDA seed
        ADD #7
randMod
        COMP #10
        JLT randDone
        SUB #10
        J randMod
randDone
        STA seed
        RSUB

. ---------------------------
secret   WORD 0
userNum  WORD 0
tmpChar  WORD 0
seed     WORD 0

. ---------------------------
. SPOROČILA ZA IZPIS (ASCII)
msgHigh BYTE C'Too High'
	BYTE 10
txtHend EQU *
msgHlen EQU txtHend - msgHigh

msgLow  BYTE C'Too Low'
	BYTE 10
txtLend EQU *
msgLlen EQU txtLend - msgLow

msgWin  BYTE C'Correct, you won! Now guess again...'
	BYTE 10
txtWend EQU *
msgWlen EQU txtWend - msgWin
