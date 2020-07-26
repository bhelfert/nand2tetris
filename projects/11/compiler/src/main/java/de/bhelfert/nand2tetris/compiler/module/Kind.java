package de.bhelfert.nand2tetris.compiler.module;

public enum Kind {

    STATIC(Segment.STATIC),
    FIELD(Segment.THIS),
    ARG(Segment.ARGUMENT),
    VAR(Segment.LOCAL);

    private final Segment segment;

    Kind(Segment segment) {
        this.segment = segment;
    }

    public Segment segment() {
        return segment;
    }
}
