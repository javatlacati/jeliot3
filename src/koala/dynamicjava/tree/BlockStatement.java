/*
 * DynamicJava - Copyright (C) 1999-2001
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Dyade shall not be
 * used in advertising or otherwise to promote the sale, use or other
 * dealings in this Software without prior written authorization from
 * Dyade.
 *
 */

package koala.dynamicjava.tree;

import java.util.List;

import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This class represents the block statement nodes of the syntax tree
 *
 * @author  Stephane Hillion
 * @version 1.0 - 1999/04/30
 */

public class BlockStatement extends Statement {
    /**
     * The creationType property name
     */
    public final static String STATEMENTS = "statements";

    /**
     * The list of the statements contained in this block
     */
    private List statements;
    
    /**
     * Creates a new block statement
     * @param stmts the list of the statements contained in this block
     */
    public BlockStatement(List stmts) {
	this(stmts, null, 0, 0, 0, 0);
    }

    /**
     * Creates a new block statement
     * @param stmts the list of the statements contained in this block
     * @param fn    the filename
     * @param bl    the begin line
     * @param bc    the begin column
     * @param el    the end line
     * @param ec    the end column
     * @exception IllegalArgumentException if stmts is null
     */
    public BlockStatement(List stmts, String fn, int bl, int bc, int el, int ec) {
	super(fn, bl, bc, el, ec);

	if (stmts == null) throw new IllegalArgumentException("stmts == null");

	statements = stmts;
    }

    /**
     * Returns the statements contained in this block
     */
    public List getStatements() {
	return statements;
    }

    /**
     * Sets the statements contained in this block
     * @exception IllegalArgumentException if l is null
     */
    public void setStatements(List l) {
	if (l == null) throw new IllegalArgumentException("l == null");

	firePropertyChange(STATEMENTS, statements, statements = l);
    }

    /**
     * Allows a visitor to traverse the tree
     * @param visitor the visitor to accept
     */
    public Object acceptVisitor(Visitor visitor) {
	return visitor.visit(this);
    }
}
