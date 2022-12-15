package org.netbeans.modules.php.blade.editor.formatter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.editor.indent.spi.Context;
import org.netbeans.modules.php.blade.editor.lexer.BladeLexerUtils;
import org.netbeans.modules.php.blade.editor.lexer.BladeTokenId;
import org.netbeans.modules.php.blade.editor.parsing.BladeParserResult;
import org.netbeans.spi.lexer.MutableTextInput;


/**
 * TODO INCOMPLETE CURRENT ISSUES html indent
 *
 * @author bhaidu
 */
public class TokenFormatter {

    protected static final String TEMPLATE_HANDLER_PROPERTY = "code-template-insert-handler";
    private static final String EMPTY_STRING = "";
    private static final Logger LOGGER = Logger.getLogger(TokenFormatter.class.getName());
    // it's for testing
    private static int unitTestCarretPosition = -1;

    public TokenFormatter() {
    }

    protected static void setUnitTestCarretPosition(int unitTestCarretPosition) {
        TokenFormatter.unitTestCarretPosition = unitTestCarretPosition;
    }

    protected static class DocumentOptions {

        public int continualIndentSize;
        public int initialIndent;
        public int indentSize;
        public int indentArrayItems;
        public int margin;
        public int tabSize;
        public boolean expandTabsToSpaces;

        public DocumentOptions(BaseDocument doc) {
            CodeStyle codeStyle = CodeStyle.get(doc);
            continualIndentSize = codeStyle.getContinuationIndentSize();
            initialIndent = codeStyle.getInitialIndent();
            indentSize = codeStyle.getIndentSize();
            margin = codeStyle.getRightMargin();
            tabSize = codeStyle.getTabSize();
            expandTabsToSpaces = codeStyle.expandTabToSpaces();
        }

    }

    public void reformat(final Context formatContext, ParserResult info) {
        final BaseDocument doc = (BaseDocument) formatContext.document();
        final BladeParserResult bladeParseResult = ((BladeParserResult) info);
        final DocumentOptions docOptions = new DocumentOptions(doc);
        
        doc.runAtomic(new Runnable() {
            @Override
            public void run() {
                final AtomicLong start = new AtomicLong(System.currentTimeMillis());
                final boolean templateEdit = GsfUtilities.isCodeTemplateEditing(doc);
                JTextComponent lastFocusedComponent = templateEdit ? EditorRegistry.lastFocusedComponent() : null;
                final int caretOffset = lastFocusedComponent != null
                        ? lastFocusedComponent.getCaretPosition()
                        : unitTestCarretPosition == -1 ? 0 : unitTestCarretPosition;
                FormatVisitor fv = new FormatVisitor(doc, docOptions, caretOffset, formatContext.startOffset(), formatContext.endOffset());
                bladeParseResult.getProgram().accept(fv);
                final List<FormatToken> formatTokens = fv.getFormatTokens();

                if (LOGGER.isLoggable(Level.FINE)) {
                    long end = System.currentTimeMillis();
                    LOGGER.log(Level.FINE, "Creating formating stream took: {0} ms", (end - start.get()));
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    TokenSequence<? extends BladeTokenId> ts = BladeLexerUtils.getBladeMarkupTokenSequence(doc, 0);
                    if (ts == null) {
                        return;
                    }
                    LOGGER.log(Level.FINE, "Tokens in TS: {0}", ts.tokenCount());
                    LOGGER.log(Level.FINE, "Format tokens: {0}", formatTokens.size());
                }
                
                MutableTextInput mti = (MutableTextInput) doc.getProperty(MutableTextInput.class);
                int indent = 0;
                int index = 0;
                int htmlIndent = 0;
                int changeOffset = -1;
                FormatToken formatToken;
                FormatToken lastFormatToken = null;
                int indentSize = docOptions.indentSize;
                try {
                    mti.tokenHierarchyControl().setActive(false);
                    start.set(System.currentTimeMillis());
                    while (index < formatTokens.size()) {
                        formatToken = formatTokens.get(index);
                        FormatToken.Kind id = formatToken.getId();
                        changeOffset = formatToken.getOffset();
                        int totalIndent = 0;
                        switch (id) {
                            case WHITESPACE_BEFORE_DIRECTIVE_TAG:
                                break;
                            case WHITESPACE_BEFORE_DIRECTIVE_START_TAG:
                                if (formatToken instanceof FormatToken.WsDirectiveToken) {
                                    FormatToken.WsDirectiveToken wsDirectiveToken = (FormatToken.WsDirectiveToken) formatToken;
                                    boolean incrementIndent = !wsDirectiveToken.getDirective().equals("@elseif");
                                    boolean alignIdentation = wsDirectiveToken.getDirective().equals("@elseif");
                                    int wsBeforeDirective = wsDirectiveToken.getWsBefore();
                                    if (indent > 0 && indent > wsBeforeDirective) {
                                        int offsetIndent = indent - wsBeforeDirective;
                                        if (alignIdentation) {
                                            offsetIndent = Math.max(0, offsetIndent -= indentSize);
                                        }
                                        insert(changeOffset, delta, new String(new char[offsetIndent]).replace("\0", " "));
                                    }
                                    if (incrementIndent){
                                        indent += indentSize;
                                    }
                                }
                                break;
                            case WHITESPACE_BEFORE_DIRECTIVE_ENDTAG:
                                if (formatToken instanceof FormatToken.WsDirectiveToken) {
                                    FormatToken.WsDirectiveToken wsDirectiveToken = (FormatToken.WsDirectiveToken) formatToken;
                                    int wsBeforeDirective = wsDirectiveToken.getWsBefore();
                                    indent = Math.max(0, indent -= indentSize);
                                    if (indent > 0 && indent > wsBeforeDirective) {
                                        insert(changeOffset, delta, new String(new char[indent - wsBeforeDirective]).replace("\0", " "));
                                    }
                                }
                                break;  
                            case WHITESPACE_BEFORE_INLINE_DIRECTIVE_TAG:
                                if (formatToken instanceof FormatToken.WsDirectiveToken) {
                                    FormatToken.WsDirectiveToken wsDirectiveToken = (FormatToken.WsDirectiveToken) formatToken;
                                    int wsBeforeDirective = wsDirectiveToken.getWsBefore();
                                    if (indent > 0 && indent > wsBeforeDirective) {
                                        int offsetIndent = indent - wsBeforeDirective;
                                        if (wsDirectiveToken.getDirective().equals("@else")) {
                                            offsetIndent = Math.max(0, offsetIndent -= indentSize);
                                        }
                                        insert(changeOffset, delta, new String(new char[offsetIndent]).replace("\0", " "));
                                    }
                                 }
                                break;
                            case WHITESPACE_BEFORE_INLINE_DIRECTIVE_START_TAG:
                                break;
                            case WHITESPACE_BEFORE_HTML:
                                //for the moment we will not complicate with indenting html also
                                 if (lastFormatToken != null && lastFormatToken.getId() == FormatToken.Kind.WHITESPACE_AFTER_ECHO) {
                                     break;
                                }
                                int suggestedIndent = suggestedIndent(changeOffset);
                                if (suggestedIndent < indent){
                                    //blocking html indent until fixes to inline attributes are fixed
                                    //insert(changeOffset - 1, delta, new String(new char[indent]).replace("\0", " "));
                                }
                                break;
                            case WHITESPACE_BEFORE_DIRECTIVE_PAREN:
                                if (lastFormatToken != null
                                        && lastFormatToken.isWhitespace()
                                        && lastFormatToken.getOldText().length() > 0) {
                                    int whitespaceOffset = lastFormatToken.getOffset();
                                    int xxx = 1;
                                    //replace(whitespaceOffset, delta, changeOffset - whitespaceOffset, "");
                                }
                                break;
                            case WHITESPACE_BEFORE_BLADE_PHP:
                                break;
                            case WHITESPACE_BEFORE_BLADE_PHP_BODY:
                                htmlIndent = suggestedIndent(changeOffset);
                                totalIndent = indent + htmlIndent;
                                FormatToken.PhpBladeToken phpFormatToken = (FormatToken.PhpBladeToken) formatToken;
                                String phpCode = phpFormatToken.getText();
                                int endOffset = changeOffset + phpCode.length();
                                int nrLines = LineDocumentUtils.getLineCount(doc, changeOffset, endOffset);
                                try {
                                    int lineIndex = LineDocumentUtils.getLineIndex(doc, changeOffset);
                                    int startLine = LineDocumentUtils.getLineStartFromIndex(doc, lineIndex);
                                    int wordStart = LineDocumentUtils.getWordStart(doc, startLine);
                                    String word = LineDocumentUtils.getWord(doc, wordStart);

                                    for (int i = 0; i < nrLines; i++) {
                                        startLine = LineDocumentUtils.getLineStartFromIndex(doc, lineIndex);

                                        if (LineDocumentUtils.isLineWhitespace(doc, startLine)) {
                                            lineIndex++;
                                            continue;
                                        }

                                        wordStart = LineDocumentUtils.getWordStart(doc, startLine);
                                        word = LineDocumentUtils.getWord(doc, wordStart);
                                        if (word.equals("\n")) {
                                            wordStart = LineDocumentUtils.getNextWordStart(doc, wordStart);
                                            word = LineDocumentUtils.getWord(doc, wordStart);
                                            startLine = wordStart;
                                        }

                                        int diffOffset = totalIndent - word.length();

                                        if (diffOffset > 0 || word.trim().length() > 0) {
                                            String whitespace = new String(new char[diffOffset]).replace("\0", " ");
                                            doc.insertString(wordStart, whitespace, null);
                                            delta += whitespace.length();
                                        }
                                        lineIndex++;
                                    }
                                } catch (BadLocationException ex) {

                                }
                                int xxx = 1;
                                break;
                        }

                        //countSpaces = 3;
                        //replace(formatToken.getOffset(), countSpaces, "  ");
                        lastFormatToken = formatToken;
                        index++;
                    }

                } finally {
                    mti.tokenHierarchyControl().setActive(true);
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    long end = System.currentTimeMillis();
                    LOGGER.log(Level.FINE, "Applaying format stream took: {0} ms", (end - start.get())); // NOI18N
                }
            }

            private int delta = 0;
            private int indent = 0;
            
            private int suggestedIndent(int changeOffset) {
                @SuppressWarnings("unchecked")
                Map<Integer, Integer> suggestedLineIndents = (Map<Integer, Integer>) doc.getProperty("AbstractIndenter.lineIndents"); // NOI18N
                try {
                    int lineNumber = LineDocumentUtils.getLineIndex(doc, changeOffset);
                    Integer suggestedIndent = suggestedLineIndents != null
                            ? suggestedLineIndents.get(lineNumber)
                            : Integer.valueOf(0);
                    if (suggestedIndent == null) {
                        suggestedIndent = suggestedLineIndents.get(lineNumber + 1) != null
                                ? suggestedLineIndents.get(lineNumber + 1)
                                : Integer.valueOf(0);
                    }
                    return suggestedIndent;
                } catch (BadLocationException ex) {

                }
                return 0;
            }

            /**
             * for the moment it can be dangerous
             * we will stick just to indenting
             */
            private void replace(int offset, int deltaOffset, int vlength, String newString) {
                try {
                    String oldText = doc.getText(offset + deltaOffset, vlength);
                    if (newString.equals(oldText)) {
                        return;
                    }
                    doc.remove(offset + deltaOffset, vlength);
                    delta -= vlength;
                } catch (BadLocationException ex) {
                    LOGGER.log(Level.INFO, null, ex);
                }
            }

            private void insert(int offset, int deltaOffset, String newString) {
                try {
                    String oldText = doc.getText(offset + deltaOffset, newString.length());
                    if (oldText.length() > 0 && oldText.charAt(0) == '\n') {
                        offset += 1;
                    }
                    delta += newString.length();
                    doc.insertString(offset + deltaOffset, newString, null);
                } catch (BadLocationException ex) {
                    LOGGER.log(Level.INFO, null, ex);
                }
            }

            private boolean isOnSameLine(int offset1, int offset2) {
                int startLine = LineDocumentUtils.getLineStart(doc, offset1);
                int endLine = LineDocumentUtils.getLineStart(doc, offset2);
                return startLine == endLine;
            }
        });
    }

    private static class Whitespace {

        int lines;
        int spaces;

        public Whitespace(int lines, int spaces) {
            this.lines = lines;
            this.spaces = spaces;
        }

    }

}
