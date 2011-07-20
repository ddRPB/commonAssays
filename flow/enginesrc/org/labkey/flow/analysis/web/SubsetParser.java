/*
 * Copyright (c) 2011 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.flow.analysis.web;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import junit.framework.Assert;
import org.junit.Test;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.PopulationName;
import org.labkey.flow.analysis.web.SubsetExpression.*;

import static org.labkey.flow.analysis.web.SubsetExpression.*;

public class SubsetParser
{
    enum Tok
    {
        name,
        nameDivider,
        escapeStart,
        escapeEnd,
        opOr,
        opAnd,
        opNot,
        opLParen,
        opRParen,
        eof
    }

    static class Token
    {
        public Token(Tok tk, String text)
        {
            _tk = tk;
            _text = text;
        }
        public Token(Tok tk, char c)
        {
            _tk = tk;
            _text = String.valueOf(c);
        }
        
        Tok _tk;
        String _text;

        @Override
        public String toString()
        {
            return String.format("tok=%s, text=%s", _tk, _text);
        }
    }

    private static final Token EOF = new Token(Tok.eof, "");

    private Lexer _lexer;
    private Token _lookahead;

    public SubsetParser(String str)
    {
        _lexer = new Lexer(str);
    }

    private Token match(Tok tk)
    {
        if (test(tk))
        {
            Token ret = _lookahead;
            _lookahead = null;
            return ret;
        }
        throw new FlowException(String.format("Expected '%s', found '%s' at index %d of '%s'", tk, _lookahead, _lexer._lookahead, _lexer._str));
    }

    private boolean test(Tok tk)
    {
        if (_lookahead == null)
            _lookahead = _lexer.next();
        return _lookahead != null && _lookahead._tk == tk;
    }

    // full-subset: subset eof
    public final SubsetSpec parseFullSubset()
    {
        SubsetExpression.SubsetTerm subset = parseSubset();
        match(Tok.eof);
        return subset.getSpec();
    }

    // full-expression: expression eof
    public final SubsetExpression parseFullExpression()
    {
        SubsetExpression expr = parseExpression();
        match(Tok.eof);
        return expr;
    }

    // subset: subsetPart ('/' subset)*
    private final SubsetTerm parseSubset()
    {
        SubsetTerm part = parseSubsetPart();
        while (test(Tok.nameDivider))
        {
            match(Tok.nameDivider);
            SubsetTerm next = parseSubsetPart();
            SubsetSpec spec;
            if (next._spec.isExpression())
                spec = new SubsetSpec(part._spec, next._spec.getExpression());
            else
                spec = new SubsetSpec(part._spec, next._spec.getPopulationName());
            part = new SubsetTerm(spec);
        }

        return part;
    }

    // subsetPart: string | expression
    private final SubsetTerm parseSubsetPart()
    {
        SubsetSpec subset;
        if (test(Tok.name))
        {
            Token part = match(Tok.name);
            subset = new SubsetSpec(null, PopulationName.fromString(part._text));
        }
        else if (test(Tok.opLParen))
        {
            SubsetExpression expr = parseExpression();
            subset = new SubsetSpec(null, expr);
        }
        else
            throw new FlowException("Expected subset name or expression");
        return new SubsetTerm(subset);
    }

    // expression: '(' and ')'
    private final SubsetExpression parseExpression()
    {
        match(Tok.opLParen);
        SubsetExpression expr = parseAnd();
        match(Tok.opRParen);
        return expr;
    }

    // and: or '|' and | or
    private final SubsetExpression parseAnd()
    {
        SubsetExpression left = parseOr();
        if (!test(Tok.opAnd))
            return left;
        match(Tok.opAnd);
        SubsetExpression right = parseAnd();
        return new AndTerm(left, right);
    }

    // or: not '&' or | not
    private final SubsetExpression parseOr()
    {
        SubsetExpression left = parseNot();
        if (!test(Tok.opOr))
            return left;
        match(Tok.opOr);
        SubsetExpression right = parseOr();
        return new OrTerm(left, right);
    }

    // not: '!' atom | atom
    private final SubsetExpression parseNot()
    {
        if (!test(Tok.opNot))
            return parseAtom();
        match(Tok.opNot);
        SubsetExpression expr = parseAtom();
        return new NotTerm(expr);
    }

    // NOTE: we are resolving an ambiguity in the grammar by giving nested expressions precedence over subset expressions
    // For example, "(A&(B|C))" is parsed as 'Subset(nullParent, And("A", Or("B", "C")))' instead of 'Subset(nullParent, And("A", Subset(nullParent, Or("B", "C")))'
    // atom: expression | subset
    private final SubsetExpression parseAtom()
    {
        if (!test(Tok.opLParen))
            return parseSubset();

        SubsetExpression expr = parseExpression();
        expr.setGrouped(true);
        return expr;
    }

    private static class Lexer implements Iterator<Token>
    {
        /** The last recognized token. */
        Token _token = null;
        /** Lookahead, if any. */
        int _lookahead = -1;
        /** Current index. */
        int _index = -1;
        /** The source string. */
        String _str = null;
        /** Are we inside an escape. */
        boolean _escaped = false;

        static Map<Character, Tok> _charTok = new HashMap<Character, Tok>();
        static
        {
            _charTok.put('/', Tok.nameDivider);
            _charTok.put(PopulationName.ESCAPE_START, Tok.escapeStart);
            _charTok.put(PopulationName.ESCAPE_END, Tok.escapeEnd);
            _charTok.put('|', Tok.opOr);
            _charTok.put('&', Tok.opAnd);
            _charTok.put('!', Tok.opNot);
            _charTok.put('(', Tok.opLParen);
            _charTok.put(')', Tok.opRParen);
        }

        Lexer(String str)
        {
            if (str == null)
                str = "";
            _str = str;
            _lookahead = 0;
        }

        void reset() throws IOException
        {
            if (_index >= _str.length())
                throw new IllegalStateException("EOF");
            _token = null;
            _index = _lookahead;
            if (_index == -1)
                read();
        }

        void read() throws IOException
        {
            if (_index >= _str.length() || _lookahead >= _str.length())
                throw new IllegalStateException("EOF");
            _lookahead++;
        }

        char lookahead()
        {
            return _str.charAt(_lookahead);
        }

        Tok lookaheadTok()
        {
            return _charTok.get(lookahead());
        }

        String text()
        {
            return _str.substring(_index, _lookahead);
        }

        void lex() throws IOException
        {
            reset();

            if (_lookahead == _str.length())
            {
                _token = EOF;
                return;
            }

            Tok tok = lookaheadTok();
            if (tok != null && tok != Tok.escapeStart)
            {
                _token = new Token(tok, lookahead());
                read();
                return;
            }

            if (tok == Tok.escapeStart)
                _escaped = true;

            // Read a string of characters until we reach another token or escapeEnd.
            // The escapeEnd character itself may be backslash-escaped.
            while (true)
            {
                read();

                if (_lookahead == _str.length())
                    break;

                tok = lookaheadTok();
                if (tok != null)
                {
                    if (tok == Tok.eof)
                        break;

                    if (tok == Tok.escapeStart)
                        _escaped = true;

                    if (_escaped)
                    {
                        if (tok == Tok.escapeEnd && _str.charAt(_lookahead-1) != '\\')
                        {
                            _escaped = false;
                            read();
                            break;
                        }
                    }
                    else if (tok != Tok.escapeEnd)
                        break;
                }
            }

            // populate the name token
            _token = new Token(Tok.name, text());
        }

        public boolean hasNext()
        {
            if (_token != null)
                return true;
            if (_index == _str.length())
                return false;
            try
            {
                lex();
            }
            catch (IOException ioe)
            {
                throw new FlowException("Error", ioe);
            }
            return true;
        }

        public Token next()
        {
            if (hasNext())
            {
                Token next = _token;
                _token = null;
                return next;
            }
            return EOF;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class TestLexer extends Assert
    {
        private void assertToken(Token expected, Token actual)
        {
            assertEquals(expected._tk, actual._tk);
            assertEquals(expected._text, actual._text);
        }

        @Test
        public void testLexNull() throws Exception
        {
            Lexer lexer = new Lexer(null);
            assertToken(EOF, lexer.next());
            assertFalse(lexer.hasNext());
        }

        @Test
        public void testLexEmpty() throws Exception
        {
            Lexer lexer = new Lexer("");
            assertToken(EOF, lexer.next());
            assertFalse(lexer.hasNext());
        }

        @Test
        public void testLexName()
        {
            Lexer lexer = new Lexer("one name");
            assertToken(new Token(Tok.name, "one name"), lexer.next());
            assertToken(EOF, lexer.next());
            assertFalse(lexer.hasNext());
        }

        @Test
        public void testLexNames()
        {
            Lexer lexer = new Lexer("lots/of/names");
            assertToken(new Token(Tok.name, "lots"), lexer.next());
            assertToken(new Token(Tok.nameDivider, "/"), lexer.next());
            assertToken(new Token(Tok.name, "of"), lexer.next());
            assertToken(new Token(Tok.nameDivider, "/"), lexer.next());
            assertToken(new Token(Tok.name, "names"), lexer.next());
            assertToken(EOF, lexer.next());
            assertFalse(lexer.hasNext());
        }

        @Test
        public void testLexEscapedName()
        {
            Lexer lexer = new Lexer("{this/is a (name)}");
            assertToken(new Token(Tok.name, "{this/is a (name)}"), lexer.next());
            assertToken(EOF, lexer.next());
            assertFalse(lexer.hasNext());
        }

        @Test
        public void testLexEscapedName2()
        {
            Lexer lexer = new Lexer("this {/is a (name)}");
            assertToken(new Token(Tok.name, "this {/is a (name)}"), lexer.next());
            assertToken(EOF, lexer.next());
        }

        @Test
        public void testLexEscapedNameStart()
        {
            Lexer lexer = new Lexer("{this {/is a name}");
            assertToken(new Token(Tok.name, "{this {/is a name}"), lexer.next());
            assertToken(EOF, lexer.next());
        }

        @Test
        public void testLexEscapedNameEnd()
        {
            Lexer lexer = new Lexer("{this /is a}name}");
            assertToken(new Token(Tok.name, "{this /is a}"), lexer.next());
            assertToken(new Token(Tok.name, "name}"), lexer.next());
            assertToken(EOF, lexer.next());
        }

        //@Test
        // The escaped escapedEnd doesn't work yet
        public void testLexEscapedNameEscapedEnd()
        {
            Lexer lexer = new Lexer("{this {/is a\\}name}");
            assertToken(new Token(Tok.name, "{this {/is a}name}"), lexer.next());
            assertToken(EOF, lexer.next());
        }

        @Test
        public void testLexOperators()
        {
            Lexer lexer = new Lexer("a&b|c/(!d)");
            assertToken(new Token(Tok.name, "a"), lexer.next());
            assertToken(new Token(Tok.opAnd, "&"), lexer.next());
            assertToken(new Token(Tok.name, "b"), lexer.next());
            assertToken(new Token(Tok.opOr, "|"), lexer.next());
            assertToken(new Token(Tok.name, "c"), lexer.next());
            assertToken(new Token(Tok.nameDivider, "/"), lexer.next());
            assertToken(new Token(Tok.opLParen, "("), lexer.next());
            assertToken(new Token(Tok.opNot, "!"), lexer.next());
            assertToken(new Token(Tok.name, "d"), lexer.next());
            assertToken(new Token(Tok.opRParen, ")"), lexer.next());
            assertToken(EOF, lexer.next());
        }

    }

    public static class TestParser extends Assert
    {
        void assertBinaryExpr(BinaryTerm expected, BinaryTerm actual)
        {
            assertExpr(expected._left, actual._left);
            assertExpr(expected._right, actual._right);
        }

        void assertNotExpr(NotTerm expected, NotTerm actual)
        {
            assertExpr(expected._term, actual._term);
        }

        void assertSubsetExpr(SubsetTerm expected, SubsetTerm actual)
        {
            assertEquals(expected._spec, actual._spec);
        }

        void assertExpr(SubsetExpression expected, SubsetExpression actual)
        {
            assertEquals(expected.getClass(), actual.getClass());
            if (expected instanceof BinaryTerm)
                assertBinaryExpr((BinaryTerm)expected, (BinaryTerm)actual);
            else if (expected instanceof NotTerm)
                assertNotExpr((NotTerm)expected, (NotTerm)actual);
            else if (expected instanceof SubsetTerm)
                assertSubsetExpr((SubsetTerm)expected, (SubsetTerm)actual);
            else
                fail("Unexpected type: " + actual.getClass());
        }

        @Test
        public void testSubset()
        {
            SubsetParser parser = new SubsetParser("A");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset("A"), expr);
        }

        @Test
        public void testSubsets()
        {
            SubsetParser parser = new SubsetParser("A/B");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset("A", "B"), expr);
        }

        @Test
        public void testRootExpression()
        {
            SubsetParser parser = new SubsetParser("(A/B)");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset(null, Subset("A", "B")), expr);
        }

        @Test
        public void testSubsetAndExpression()
        {
            SubsetParser parser = new SubsetParser("X/(A/B)");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset("X", Subset("A", "B")), expr);
        }

        @Test
        public void testUnexpectedParen()
        {
            SubsetParser parser = new SubsetParser("X/Y(A/B)");
            try
            {
                parser.parseFullSubset();
                fail("Expected to throw parse exception");
            }
            catch (FlowException e)
            {
                assertEquals("Expected 'eof', found 'tok=opLParen, text=(' at index 4 of 'X/Y(A/B)'", e.getMessage());
            }
        }

        @Test
        public void testUnexpectedNot()
        {
            SubsetParser parser = new SubsetParser("!A");
            try
            {
                SubsetExpression expr = parser.parseSubset();
                fail("Expected to throw parse exception");
            }
            catch (FlowException e)
            {
                assertEquals("Expected subset name or expression", e.getMessage());
            }
        }

        @Test
        public void testNot()
        {
            SubsetParser parser = new SubsetParser("(!A/B)");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset(null, Not(Subset("A", "B"))), expr);
        }

        @Test
        public void testOr()
        {
            SubsetParser parser = new SubsetParser("(A/B|C/D)");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset(null, Or(Subset("A", "B"), Subset("C", "D"))), expr);
        }

        @Test
        public void testAnd()
        {
            SubsetParser parser = new SubsetParser("(A/B&C/D&E)");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset(null, And(Subset("A", "B"), And(Subset("C", "D"), Subset("E")))), expr);
        }

        @Test
        public void testPrecendence()
        {
            {
                SubsetParser parser = new SubsetParser("(A|B&C)");
                SubsetExpression expr = parser.parseSubset();
                assertExpr(Subset(null, And(Or(Subset("A"), Subset("B")), Subset("C"))), expr);
            }

            // use grouping to change precedence
            {
                SubsetParser parser = new SubsetParser("(A|(B&C))");
                SubsetExpression expr = parser.parseSubset();
                assertExpr(Subset(null, Or(Subset("A"), Group(And(Subset("B"), Subset("C"))))), expr);
            }
        }

        @Test
        public void testNestedExpression()
        {
            // NOTE: Our grammar doesn't allow expressions containing another subset expression at the root.
            // The nested expression must start with a population name.
            // For example, "(A&(B|C))" is parsed as:
            //   And(Subset("A"), Group(Or(Subset("B"), Subset("C"))))
            // instead of
            //   And(Subset("A"), Subset(Or(Subset("B"), Subset("C")))
            {
                SubsetParser parser = new SubsetParser("(A&(B|C))");
                SubsetExpression expr = parser.parseExpression();
                assertExpr(And(Subset("A"), Group(Or(Subset("B"), Subset("C")))), expr);
            }

            // However, this is a subset expression since it begins with child population name 'X'.
            {
                SubsetParser parser = new SubsetParser("(A&X/(B|C))");
                SubsetExpression expr = parser.parseExpression();
                assertExpr(And(Subset("A"), Subset("X", Or(Subset("B"), Subset("C")))), expr);
            }
        }

        @Test
        public void testNestedExpression2()
        {
            SubsetParser parser = new SubsetParser("(A&!(B|C)|(D&E))");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset(null,
                    And(
                            Subset("A"),
                            Or(
                                Not(Group(Or(Subset("B"), Subset("C")))),
                                Group(And(Subset("D"), Subset("E")))))),
                    expr);
        }

        @Test
        public void testNestedSubsets()
        {
            SubsetParser parser = new SubsetParser("(A/(B|C/(!D)))");
            SubsetExpression expr = parser.parseSubset();
            assertExpr(Subset(null, Subset("A", Or(Subset("B"), Subset("C", Not(Subset("D")))))), expr);
        }

    }
}
