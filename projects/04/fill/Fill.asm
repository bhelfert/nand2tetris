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

(READ_KEYBOARD)
                @i                
                M=0              // i = 0
                @KBD
                D=M              // D = ASCII code at RAM[24576]
                @WHITEN_SCREEN
                D;JEQ            // if (D == 0) goto WHITEN_SCREEN
                @color
                M=-1             // color = black
(FILL_SCREEN)
                @i                
                D=M              // D = i
                @8192
                D=D-A            // D = i - 8192
                @READ_KEYBOARD
                D;JEQ            // if (i - 8192) == 0 goto INFINITE_LOOP
                @SCREEN
                D=A              // D = 16384
                @i
                D=D+M            // D = 16384 + i
                @pixel_address
                M=D              // pixel_address = D
                @color
                D=M              // D = color
                @pixel_address
                A=M              // A = pixel_address
                M=D              // RAM[A] = color
                @i
                M=M+1            // i = i + 1
                @FILL_SCREEN
                0;JMP                
(WHITEN_SCREEN)
                @color
                M=0              // color = white
                @FILL_SCREEN
                0;JMP