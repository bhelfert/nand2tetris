// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.

(INFINITE_LOOP)
                @i                
                M=0              // i = 0
                @KBD
                D=M              // D = ASCII code at RAM[24576]
                @NO_KEY_PRESSED
                D;JEQ            // if (D == 0) goto NO_KEY_PRESSED
(KEY_PRESSED)                    // blacken screen
                @i                
                D=M              // D = i
                @8192
                D=D-A            // D = i - 8192
                @INFINITE_LOOP
                D;JEQ            // if (i - 8192) == 0 goto INFINITE_LOOP
                @SCREEN
                D=A              // D = 24576
                @i
                A=D+M            // A = 24576 + i
                M=-1             // RAM[A] = black
                @i
                M=M+1            // i = i + 1
                @KEY_PRESSED
                0;JMP                
(NO_KEY_PRESSED)                 // whiten screen
                @i                
                D=M              // D = i
                @8192
                D=D-A            // D = i - 8192
                @INFINITE_LOOP
                D;JEQ            // if (i - 8192) == 0 goto INFINITE_LOOP
                @SCREEN
                D=A              // D = 24576
                @i
                A=D+M            // A = 24576 + i
                M=0              // RAM[A] = white
                @i
                M=M+1            // i = i + 1
                @NO_KEY_PRESSED
                0;JMP            
