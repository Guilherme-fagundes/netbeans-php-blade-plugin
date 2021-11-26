///*
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// *
// * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
// *
// * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
// * Other names may be trademarks of their respective owners.
// *
// * The contents of this file are subject to the terms of either the GNU
// * General Public License Version 2 only ("GPL") or the Common
// * Development and Distribution License("CDDL") (collectively, the
// * "License"). You may not use this file except in compliance with the
// * License. You can obtain a copy of the License at
// * http://www.netbeans.org/cddl-gplv2.html
// * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
// * specific language governing permissions and limitations under the
// * License.  When distributing the software, include this License Header
// * Notice in each file and include the License file at
// * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
// * particular file as subject to the "Classpath" exception as provided
// * by Oracle in the GPL Version 2 section of the License file that
// * accompanied this code. If applicable, add the following below the
// * License Header, with the fields enclosed by brackets [] replaced by
// * your own identifying information:
// * "Portions Copyrighted [year] [name of copyright owner]"
// *
// * If you wish your version of this file to be governed by only the CDDL
// * or only the GPL Version 2, indicate your decision by adding
// * "[Contributor] elects to include this software in this distribution
// * under the [CDDL or GPL Version 2] license." If you do not indicate a
// * single choice of license, a recipient has the option to distribute
// * your version of this file under either the CDDL, the GPL Version 2 or
// * to extend the choice of license to its licensees as provided above.
// * However, if you add GPL Version 2 code and therefore, elected the GPL
// * Version 2 license, then the option applies only if the new code is
// * made subject to such option by the copyright holder.
// *
// * Contributor(s):
// *
// * Portions Copyrighted 2016 Sun Microsystems, Inc.
// */
//package org.netbeans.modules.php.blade.editor.embeddings;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import org.netbeans.modules.php.blade.editor.gsf.BladeLanguage;
//import org.netbeans.modules.php.blade.editor.lexer.BladeTopTokenId;
//import org.netbeans.api.lexer.Token;
//import org.netbeans.api.lexer.TokenHierarchy;
//import org.netbeans.api.lexer.TokenSequence;
//import org.netbeans.modules.parsing.api.Embedding;
//import org.netbeans.modules.parsing.api.Snapshot;
//import org.netbeans.modules.parsing.spi.EmbeddingProvider;
//import org.netbeans.modules.parsing.spi.SchedulerTask;
//import org.netbeans.modules.parsing.spi.TaskFactory;
//import org.netbeans.modules.php.api.util.FileUtils;
//import static org.netbeans.modules.php.api.util.FileUtils.PHP_MIME_TYPE;
//
///**
// *
// * @author Haidu Bogdan
// */
//@EmbeddingProvider.Registration(mimeType = BladeLanguage.BLADE_MIME_TYPE, targetMimeType="text/html-xxxx")
//
//public class BladeHtmlEmbeddingProvider extends EmbeddingProvider {
//    static String HTML_MIME_TYPE = "text/html";
//    @Override
//    public List<Embedding> getEmbeddings(Snapshot snapshot) {
//        TokenHierarchy<CharSequence> th = TokenHierarchy.create(snapshot.getText(), BladeTopTokenId.language());
//        TokenSequence<BladeTopTokenId> sequence = th.tokenSequence(BladeTopTokenId.language());
//        if (sequence == null) {
//            return Collections.emptyList();
//        }
//
//        sequence.moveStart();
//        List<Embedding> embeddings = new ArrayList<>();
//
//        int offset = -1;
//        int length = 0;
//        int from = -1;
//        int len = 0;
//        while (sequence.moveNext()) {
//            Token t = sequence.token();
//            if (t.id() != BladeTopTokenId.T_PHP) {
//                if (from < 0) {
//                    from = sequence.offset();
//                }
//                len += t.length();
//                
//            } else {
//                if (from >= 0) {
//                    //lets suppose the text is always html :-(
//                    embeddings.add(snapshot.create(from, len, HTML_MIME_TYPE));
//                    //add only one virtual generated token for a sequence of PHP tokens
//                    //embeddings.add(snapshot.create("@@@", HTML_MIME_TYPE));
//                }
//
//                from = -1;
//                len = 0;
//            }
//        }
//
//        if (from >= 0) {
//            embeddings.add(snapshot.create(from, len, HTML_MIME_TYPE));
//        }
//        
//        if (embeddings.isEmpty()) {
//            return Collections.singletonList(snapshot.create("", "text/html"));
//        } else {
//            return Collections.singletonList(Embedding.create(embeddings));
//        }
//    }
//
//    @Override
//    public int getPriority() {
//        return 210;
//    }
//
//    @Override
//    public void cancel() {
//    }
//
//    public static final class Factory extends TaskFactory {
//
//        @Override
//        public Collection<SchedulerTask> create(final Snapshot snapshot) {
//            return Collections.<SchedulerTask>singletonList(new BladeHtmlEmbeddingProvider());
//        }
//    }
//}
