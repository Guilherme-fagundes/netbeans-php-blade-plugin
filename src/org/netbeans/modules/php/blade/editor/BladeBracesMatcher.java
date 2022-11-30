/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.blade.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import org.netbeans.modules.php.blade.editor.lexer.BladeLexerUtils;
import org.netbeans.modules.php.blade.editor.lexer.BladeTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.spi.editor.bracesmatching.BracesMatcher;
import org.netbeans.spi.editor.bracesmatching.BracesMatcherFactory;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;

/**
 * highlight block tags directives TODO maybe use a block scope impl
 *
 * @author Haidu Bogdan
 */
public class BladeBracesMatcher implements BracesMatcher {

    private final MatcherContext context;

    private static Collection<BladeTokenId> TOKENS_WITH_ENDTAGS = Arrays.asList(
            BladeTokenId.T_BLADE_PHP_OPEN,
            BladeTokenId.T_BLADE_IF,
            BladeTokenId.T_BLADE_FOR,
            BladeTokenId.T_BLADE_FOREACH,
            BladeTokenId.T_BLADE_SECTION //@section with second parameter can be standalone also
    );

    private static Collection<BladeTokenId> TOKEN_END_TAGS = Arrays.asList(
            BladeTokenId.T_BLADE_ENDFOREACH,
            BladeTokenId.T_BLADE_ENDFOR,
            BladeTokenId.T_BLADE_ENDIF,
            BladeTokenId.T_BLADE_ENDSECTION,
            BladeTokenId.T_BLADE_ENDPHP
    );

    private static Collection<BladeTokenId> TOKEN_OPEN_TAGS = Arrays.asList(
            BladeTokenId.T_BLADE_OPEN_ECHO,
            BladeTokenId.T_BLADE_OPEN_ECHO_ESCAPED
    );

    private static Collection<BladeTokenId> TOKEN_CLOSE_TAGS = Arrays.asList(
            BladeTokenId.T_BLADE_CLOSE_ECHO,
            BladeTokenId.T_BLADE_CLOSE_ECHO_ESCAPED
    );

    private BladeBracesMatcher(MatcherContext context) {
        this.context = context;
    }

    @Override
    public int[] findOrigin() throws InterruptedException, BadLocationException {
        int searchOffset = context.getSearchOffset();
        ((AbstractDocument) context.getDocument()).readLock();
        try {
            TokenSequence<BladeTokenId> ts = BladeLexerUtils.getBladeTokenSequence(context.getDocument());

            while (searchOffset != context.getLimitOffset()) {
                int diff = ts.move(searchOffset);
                searchOffset = searchOffset + (context.isSearchingBackward() ? -1 : +1);

                if (diff == 0 && context.isSearchingBackward()) {
                    //we are searching backward and the offset is at the token boundary
                    if (!ts.movePrevious()) {
                        continue;
                    }
                } else {
                    if (!ts.moveNext()) {
                        continue;
                    }
                }
                Token<? extends BladeTokenId> t = ts.token();
                int toffs = ts.offset();
                String tText = t.text().toString();
                BladeTokenId id = t.id();
                BladeTokenId pairToken = id.pair;
                BladeTokenId openPair;
                
                if (pairToken != null || (openPair = id.getOpenPair(id)) != null) {
                    return new int[]{toffs, toffs + tText.length()};
                } else if (TOKENS_WITH_ENDTAGS.contains(id)) {
                    return new int[]{toffs, toffs + tText.trim().length()};
                } else if (BladeSyntax.DIRECTIVES_WITH_ENDTAGS.contains(tText.trim())) {
                    return new int[]{toffs, toffs + tText.trim().length()};
                } else if (BladeSyntax.CONDITIONAL_DIRECTIVES.contains(tText.trim())) {
                    return new int[]{toffs, toffs + tText.trim().length()};
                } else if (id == BladeTokenId.T_BLADE_LPAREN) {
                    //we will try to get the directive tag
                    ts.move(searchOffset - 1);
                    if (!ts.movePrevious()) {
                        continue;
                    }
                    Token<? extends BladeTokenId> dToken = ts.token();
                    int directiveOffs = ts.offset();
                    String dText = dToken.text().toString();
                    if (TOKENS_WITH_ENDTAGS.contains(dToken.id())) {
                        return new int[]{directiveOffs, directiveOffs + dText.trim().length()};
                    }
                } else if (TOKEN_END_TAGS.contains(id)) {
                    return new int[]{toffs, toffs + tText.length()};
                } else if (tText.trim().startsWith("@end")) {
                    String tagOpen = "@" + tText.trim().substring(4);
                    if (BladeSyntax.DIRECTIVES_WITH_ENDTAGS.contains(tagOpen)) {
                        return new int[]{toffs, toffs + tText.trim().length()};
                    }
                    return null;
                }
            }
            return null;
        } finally {
            ((AbstractDocument) context.getDocument()).readUnlock();
        }
    }

    @Override
    public int[] findMatches() throws InterruptedException, BadLocationException {
        int searchOffset = context.getSearchOffset();
        ((AbstractDocument) context.getDocument()).readLock();
        try {
            TokenSequence<BladeTokenId> ts = BladeLexerUtils.getBladeTokenSequence(context.getDocument());

            while (searchOffset != context.getLimitOffset()) {
                int diff = ts.move(searchOffset);
                searchOffset = searchOffset + (context.isSearchingBackward() ? -1 : +1);

                if (diff == 0 && context.isSearchingBackward()) {
                    //we are searching backward and the offset is at the token boundary
                    if (!ts.movePrevious()) {
                        continue;
                    }
                } else {
                    if (!ts.moveNext()) {
                        continue;
                    }
                }
                OffsetRange r;
                Token<? extends BladeTokenId> t = ts.token();
                String tText = t.text().toString();
                BladeTokenId id = t.id();

                BladeTokenId pairToken = id.pair;
                BladeTokenId openPair;
//                BladeTokenId closeToken = 
                //EnumMap<BracesOpen, String> enumMap = new EnumMap<>(BracesOpen.class);
                if (pairToken != null) {
                    r = BladeLexerUtils.findFwd(ts, pairToken, pairToken.fixedText());
                    return new int[]{r.getStart(), r.getEnd()};
                } else if ((openPair = id.getOpenPair(id)) != null) {
                    r = BladeLexerUtils.findBack(ts, openPair, openPair.fixedText());
                    return new int[]{r.getStart(), r.getEnd()};
                }  else if (TOKENS_WITH_ENDTAGS.contains(t.id()) || BladeSyntax.DIRECTIVES_WITH_ENDTAGS.contains(tText.trim())) {
                    String name = tText.trim().substring(1);
                    r = BladeLexerUtils.findFwd(ts, "@end" + name, tText.trim());
                    return new int[]{r.getStart(), r.getEnd()};
                } else if (TOKEN_END_TAGS.contains(id)) {
                    String tokenTest = tText.trim();
                    String name = "@" + tokenTest.substring(4);
                    Collection<String> optionalMatches = new ArrayList<String>() {
                    };
                    if (tokenTest.equals("@endif")) {
                        optionalMatches.add("@hasSection");
                        optionalMatches.add("@missingSection");
                    }
                    r = BladeLexerUtils.findBack(ts, name, tText.trim(), optionalMatches);
                    return new int[]{r.getStart(), r.getEnd()};
                } else if (BladeSyntax.CONDITIONAL_DIRECTIVES.contains(tText.trim())) {
                    Collection<String> optionalMatches = Arrays.asList("@hasSection", "@missingSection");
                    r = BladeLexerUtils.findFwd(ts, "@endif", tText.trim(), optionalMatches);
                    return new int[]{r.getStart(), r.getEnd()};
                } else if (tText.trim().startsWith("@end")) {
                    String tagOpen = "@" + tText.trim().substring(4);
                    if (BladeSyntax.DIRECTIVES_WITH_ENDTAGS.contains(tagOpen)) {
                        r = BladeLexerUtils.findBack(ts, tagOpen, tText.trim(), new ArrayList<String>() {
                        });
                        return new int[]{r.getStart(), r.getEnd()};
                    }
                    return null;
                }
            }
            return null;
        } finally {
            ((AbstractDocument) context.getDocument()).readUnlock();
        }
    }

    //factory
    public static final class Factory implements BracesMatcherFactory {

        @Override
        public BracesMatcher createMatcher(MatcherContext context) {
            return new BladeBracesMatcher(context);
        }

    }


}
