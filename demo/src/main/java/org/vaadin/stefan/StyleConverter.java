package org.vaadin.stefan;

import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.dom.CSSStyleRuleImpl;
import com.steadystate.css.dom.Property;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;
import org.w3c.css.sac.InputSource;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleSheet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class StyleConverter {
    public static void main(String[] args) throws Exception {
        Path styles = Paths.get("styles").toAbsolutePath();
        List<String> strings = Files.readAllLines(styles);

        CSSOMParser parser = new CSSOMParser(new SACParserCSS3());
        CSSStyleSheet styleSheet = parser.parseStyleSheet(new InputSource(styles.toUri().toString()), null, null);

        Map<String, String> defs = new HashMap<>();
        Set<String> usage = new HashSet<>();

        CSSRuleList rules = styleSheet.getCssRules();
        for (int i = 0; i < rules.getLength(); i++) {
            final CSSRule rule = rules.item(i);

            if (rule instanceof CSSStyleRuleImpl) {
                CSSStyleRuleImpl sRule = (CSSStyleRuleImpl) rule;

                SelectorList selectors = sRule.getSelectors();
                List<Property> properties = ((CSSStyleDeclarationImpl) sRule.getStyle()).getProperties();

                for (int j = 0; j < selectors.getLength(); j++) {
                    Selector item = selectors.item(j);
                    String prefix = item.toString().trim();
                    prefix = prefix.replace(".", "");
                    prefix = prefix.replace(" ", "_");
                    prefix = prefix.replace(">", "_LACE_BRACE_");
                    prefix = prefix.replace(":", "_COLON_");
                    prefix = prefix.replace("*", "_ASTERISK_");
                    prefix = prefix.replace("+", "_PLUS_");
                    prefix = prefix.replace("[", "_SQUARE_BRACKET_OPEN_");
                    prefix = prefix.replace("]", "_SQUARE_BRACKET_CLOSE_");
                    prefix = prefix.replace("(", "_R_BRACKET_OPEN_");
                    prefix = prefix.replace(")", "_R_BRACKET_CLOSE_");


                    for (Property property : properties) {
                        String fullName = prefix + "-" + property.getName().trim();

                        String cssText = property.getValue().getCssText();
                        if (cssText.matches("-[a-zA-Z].*")) {
                            fullName += "-" + cssText.split("\\(")[0].trim();
                        }


//                        if (cssText.startsWith("rgb") ) {
//                            fullName += "-" + cssText.split("\\(")[0].trim();
//                        }

                        if (defs.containsKey(fullName) && !defs.get(fullName).equals(cssText)) {
                            System.err.println(fullName + " already in there with diff css text: " + cssText + " vs. " + defs.get(fullName));
                        } else {
                            defs.put(fullName, cssText);
                            usage.add(item.toString() + " { " + property.getName() + ": var(--" + fullName + ", " + cssText + "); }");
                        }
                    }
                }
            }

            BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get("styles_def.html"));

            bufferedWriter.write("<custom-style>\n    <style>\n        html{\n");

            defs.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(e -> {
                try {
                    bufferedWriter.write("            --" + e.getKey() + ": " + e.getValue() + ";");
                    bufferedWriter.newLine();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            bufferedWriter.write("        }\n    </style>\n</custom-style>");
            bufferedWriter.close();

            BufferedWriter bufferedWriter2 = Files.newBufferedWriter(Paths.get("styles_usage"));
            usage.stream().sorted(Comparator.naturalOrder()).forEach(s -> {
                try {
                    bufferedWriter2.write(s);
//                    bufferedWriter2.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            bufferedWriter2.close();
        }

    }

}
