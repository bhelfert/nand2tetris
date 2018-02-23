# About

These files are part of the wonderful learning experience provided by the course [From NAND to Tetris: Building a Modern Computer from First Principles](http://www.nand2tetris.org/).

Kudos goes to all its [creators](http://www.nand2tetris.org/team.php) and especially to Noam Nisan and Shimon Schocken for the idea and providing such a great course companion book, [The Elements of Computing Systems](https://www.amazon.com/Elements-Computing-Systems-Building-Principles/dp/0262640686/ref=ed_oe_p), MIT Press.

Of course, the implementation of the project tasks you will find here are not the creators' solutions, but only mine ;).

# Hardware Platform ("Hack")

The [computer](https://github.com/sevenlist/nand2tetris/blob/master/projects/05/Computer.hdl) hardware is build from
* [Logical Gates](https://github.com/sevenlist/nand2tetris/tree/master/projects/01)
* [FullAdder, ALU, Incrementer](https://github.com/sevenlist/nand2tetris/tree/master/projects/02)
* [Memory Elements, incl. PC](https://github.com/sevenlist/nand2tetris/tree/master/projects/03)
* [CPU, Keyboard, Screen](https://github.com/sevenlist/nand2tetris/tree/master/projects/05)

using only NAND gates and data flip-flops (DFFs) as atomic, primitive, building blocks.

All chips are implemented using this [Hardware Description Language](http://www.nand2tetris.org/chapters/appendix%20A.pdf) (HDL).

# En Route: Learning The Languages

* Assembly: [Multiply two numbers](https://github.com/sevenlist/nand2tetris/blob/master/projects/04/mult/mult.asm); [blacken/whiten the screen](https://github.com/sevenlist/nand2tetris/blob/master/projects/04/fill/Fill.asm)
* Jack: ["Hello \<name\>!"](https://github.com/sevenlist/nand2tetris/tree/master/projects/09/greetme)

# Software Hierarchy

* [Assembler](https://github.com/sevenlist/nand2tetris/tree/master/projects/06/assembler)
* [Virtual Machine Translator](https://github.com/sevenlist/nand2tetris/tree/master/projects/08/vmtranslator)
* Compiler (for the [Jack](http://www.nand2tetris.org/lectures/PDF/lecture%2009%20high%20level%20language.pdf) programming language)
* Operating System / standard library for Jack