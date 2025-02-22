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
package org.netbeans.modules.php.blade.editor.embeddings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.netbeans.modules.php.blade.editor.BladeLanguage;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.EmbeddingProvider;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.netbeans.modules.php.blade.editor.lexer.BladeTokenId;

/**
 *
 * @author Haidu Bogdan
 */
@EmbeddingProvider.Registration(mimeType = BladeLanguage.BLADE_MIME_TYPE, targetMimeType="text/x-php5")
public class BladePhpEmbeddingProvider extends EmbeddingProvider {
    public static String PHP_MIME_TYPE = "text/x-php5";
    @Override
    public List<Embedding> getEmbeddings(Snapshot snapshot) {
        TokenHierarchy<CharSequence> th = TokenHierarchy.create(snapshot.getText(), BladeTokenId.language());
        TokenSequence<BladeTokenId> sequence = th.tokenSequence(BladeTokenId.language());
        if (sequence == null) {
            return Collections.emptyList();
        }

        sequence.moveStart();
        List<Embedding> embeddings = new ArrayList<>();

        int offset = 0;
        int len = 0;

        String fake;
        int phpStart = -1;
        int phpLength = 0;
        boolean prevTokenWasHtml = false;
        
        while (sequence.moveNext()) {
            Token<BladeTokenId> t = sequence.token();
            offset = sequence.offset();
            TokenId id = t.id();
            len += t.length();
            String tText = t.text().toString();
            if (len == 0){
                continue;
            }

            boolean tokenIsWhitespace = id.equals(BladeTokenId.NEWLINE) || id.equals(BladeTokenId.WHITESPACE);

            if (id == BladeTokenId.T_BLADE_PHP_OPEN) {
                //fake = new String(new char[tText.length()]).replace("\0", "@");
              //  fake = new String(new char[tText.length()]).replace("\0", " ");
              //  embeddings.add(snapshot.create(fake, PHP_MIME_TYPE));
            } else if (id == BladeTokenId.T_BLADE_PHP) {
                //SEEMS TO WORK VERY STRANGE
                fake = "<?php " +  tText + "?>" + new String(new char[3]).replace("\0", " ");
                embeddings.add(snapshot.create(fake, PHP_MIME_TYPE));
            } else if (id == BladeTokenId.T_BLADE_ENDPHP) {
//                fake = new String(new char[tText.length()]).replace("\0", "@");
               // fake = new String(new char[tText.length()]).replace("\0", " ");
               // embeddings.add(snapshot.create(fake, PHP_MIME_TYPE));
            } else if (id == BladeTokenId.T_OPEN_PHP_SCRIPT) {
                phpStart = offset;
                phpLength =  t.length();
            } else if (id == BladeTokenId.T_PHP && phpStart >= 0) {
                phpLength += t.length();
            } else if (id == BladeTokenId.T_CLOSE_PHP && phpStart >= 0) {
                phpLength += t.length();
                embeddings.add(snapshot.create(phpStart, phpLength, PHP_MIME_TYPE));
                phpStart = phpLength = 0;
            } else if (id.equals(BladeTokenId.T_HTML) || tokenIsWhitespace && prevTokenWasHtml) {
                embeddings.add(snapshot.create(offset, t.length(), PHP_MIME_TYPE));
            } else {
                    //in order to enable code completion
                fake = new String(new char[tText.length()]).replace("\0", " ");
                embeddings.add(snapshot.create(fake, PHP_MIME_TYPE));
            }
            
            prevTokenWasHtml = id.equals(BladeTokenId.T_HTML) || tokenIsWhitespace;
        }

        if (embeddings.isEmpty()) {
            return Collections.singletonList(snapshot.create("", PHP_MIME_TYPE));
        } else {
            return Collections.singletonList(Embedding.create(embeddings));
        }
    }

    @Override
    public int getPriority() {
        return 210;
    }

    @Override
    public void cancel() {
    }

    public static final class Factory extends TaskFactory {

        @Override
        public Collection<SchedulerTask> create(final Snapshot snapshot) {
            return Collections.<SchedulerTask>singletonList(new BladePhpEmbeddingProvider());
        }
    }
}
