package Checkers;

public class Move {
    Pieces piece;
    int pRow,  pCol, nRow, nCol;



    public Move(Pieces piece, int pRow, int pCol, int nRow, int nCol){
        this.piece = piece;
        this.pRow = pRow;
        this.pCol = pCol;
        this.nRow = nRow;
        this.nCol = nCol;
    }
    public Pieces getPiece() {
        return piece;
    }
    public int getpRow() {
        return pRow;
    }
    public int getpCol() {
        return pCol;
    }
    public int getnRow() {
        return nRow;
    }
    public int getnCol() {
        return nCol;
    }
}

