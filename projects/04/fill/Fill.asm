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
                @SCREEN
                D=A              // D = 16384
                @pixel_address   
                M=D              // pixel_address = D
                @KBD
                D=M              // D = ASCII code at RAM[24576]
                @WHITEN_SCREEN
                D;JEQ            // if (D == 0) goto WHITEN_SCREEN
                @color
                M=-1             // color = black
(FILL_SCREEN)
                @pixel_address                
                D=M              // D = pixel_address
                @KBD             // = @SCREEN + (256 * 512) = 16384 + 8192 = 24576 
                D=D-A            // D = pixel_address - 24576
                @READ_KEYBOARD
                D;JEQ            // if (D == 0) goto READ_KEYBOARD
                @color
                D=M              // D = color
                @pixel_address
                A=M              // A = pixel_address
                M=D              // RAM[A] = color
                @pixel_address
                M=M+1            // pixel_address = pixel_address + 1
                @FILL_SCREEN
                0;JMP                
(WHITEN_SCREEN)
                @color
                M=0              // color = white
                @FILL_SCREEN
                0;JMP