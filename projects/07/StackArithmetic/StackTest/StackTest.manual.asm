// SP = 256
@256
D=A
@SP
M=D

// push constant 17
@17
D=A
@SP
A=M
M=D   // @256 = 17

// SP += 1 = 257
@SP
M=M+1

// push constant 17
@17
D=A
@SP
A=M
M=D    // @257 = 17

// SP += 1 = 258
@SP
M=M+1

// sub (like add, with the last line changed)
@SP
M=M-1   // SP -= 1 = 257
A=M     // A = 257
D=M     // D = 17
@SP
M=M-1   // SP -= 1 = 256
A=M     // A = 256
M=M-D   // @256 = 17 - 17 = 0

// eq; true = -1, false = 0
@SP
A=M
D=M
@IF_TRUE
D;JEQ
@SP
A=M
M=0     // @256 = 0
@END_IF
0;JMP
(IF_TRUE)
@SP
A=M
M=-1    // @256 = -1
(END_IF)

// push constant 17
// push constant 16
// eq

// push constant 16
// push constant 17
// eq

// push constant 892
// push constant 891
// lt

// push constant 891
// push constant 892
// lt

// push constant 891
// push constant 891
// lt

// push constant 32767
// push constant 32766
// gt

// push constant 32766
// push constant 32767
// gt

// push constant 32766
// push constant 32766
// gt

// push constant 57
// push constant 31
// push constant 53
// add
// push constant 112
// sub
// neg
// and
// push constant 82
// or
// not