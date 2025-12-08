print	START 0

        CLEAR   X
loop    LDCH    txt, X
        WD      #1
        TIX     #txtlen
        JLT     loop

halt	J halt

txt     BYTE    C'SIC/XE'
        BYTE    10
txtend  EQU     *
txtlen  EQU     txtend - txt

        END print

