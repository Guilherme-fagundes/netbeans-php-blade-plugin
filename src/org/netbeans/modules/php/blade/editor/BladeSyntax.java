package org.netbeans.modules.php.blade.editor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

/**
 * list of blade syntax info
 * @link https://laravel.com/docs/8.x/blade
 *
 * @author bhaidu
 */
public class BladeSyntax {

    private static URL documentationUrl = null;

    static {
        try {
            documentationUrl = new URL("https://laravel.com/docs/8.x/blade"); //NOI18N
        } catch (MalformedURLException ex) {

        }
    }

    public final static Collection<String> INLINE_DIRECTIVES = Arrays.asList(
        "@include", "@includeIf", "@extends", 
        "@section" //can be inline
    );
    
    public final static Collection<String> DIRECTIVES_WITH_VIEW_PATH = Arrays.asList(
            "@include", "@includeIf", "@extends"
    );

    public static Collection<String> DIRECTIVES_WITH_ENDTAGS = Arrays.asList(
            "@php",
            "@if",
            "@for",
            "@foreach",
            "@isset",
            "@empty",
            "@production",
            "@while",
            "@section",
            "@switch",
            "@env",
            "@verbatim",
            "@unless",
            "@auth",
            "@guest",
            "@once",
            "@disk",
            "@push",
            "@prepend",
            "@error"
    );
    
    public final static Collection<String> CONDITIONAL_DIRECTIVES = Arrays.asList(
        "@hasSection", "@sectionMissing"
    );
    
    public static URL getDocumentationUrl(){
        return documentationUrl;
    }
}
