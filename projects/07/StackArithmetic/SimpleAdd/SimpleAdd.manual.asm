// SP = 256
@256
D=A
@SP
M=D

// push constant 7
@7
D=A    // D = 7
@SP    
A=M    // A = @0 = 256
M=D    // @256 = 7

// SP += 1 = 257
@SP
M=M+1

// push constant 8
@8
D=A    // D = 8
@SP
A=M    // A = @0 = 257
M=D    // @257 = 8

// SP += 1 = 258
@SP
M=M+1

// add
@SP
M=M-1   // SP -= 1 = 257
A=M     // A = 257
D=M     // D = 8
@SP
M=M-1   // SP -= 1 = 256
A=M     // A = 256
M=D+M   // @256 = 8 + 7 = 15

// SP += 1 = 257
@SP
M=M+1