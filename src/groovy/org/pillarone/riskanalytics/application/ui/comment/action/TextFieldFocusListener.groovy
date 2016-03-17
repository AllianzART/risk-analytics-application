package org.pillarone.riskanalytics.application.ui.comment.action

import com.ulcjava.base.application.ULCTextField
import com.ulcjava.base.application.event.FocusEvent
import com.ulcjava.base.application.event.IFocusListener
import com.ulcjava.base.application.util.Color
import groovy.transform.CompileStatic
import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @author fouad.jaada@intuitive-collaboration.com
 *
 * Make search field show grey hint if user clicks away without typing anything.
 * When user clicks in the field clear the hint out (unless a search filter is present).
 *
 */
@CompileStatic
class TextFieldFocusListener implements IFocusListener {
    private static Log LOG = LogFactory.getLog(TextFieldFocusListener)
    static Random rnd = new Random();
    static final String[] searchFilterTips = [
            "Tip:  Useful search prefixes include: NAME, TAG, OWNER, STATE",
            "Tip: 'NAME:xyz'  finds all items whose name CONTAINS 'xyz'",
            "Tip: '!TAG=16Q1'  finds all items  NOT  (!)  carrying TAG '16Q1'",
            "Tip: 'STATE:prod'  finds all  'in production'  models",
            "Tip:  Useful abbreviations: n (NAME), t (TAG), o (OWNER), s (STATE) ..",
            "Tip:  Other prefixes:  dn (DealName),  dt (DealTag) (Care- this is SLOW!)",
            "Tip: 'DN:commuted' finds commuted transactions (deal name contains 'commuted')",
            "Tip:  Prefix abbrs:  DEALNAME->dn, NAME->n, STATE->s, TAG->t, OWNER->o ",
            "Tip:  A COLON (:) after a prefix means 'CONTAINS'; (eg 'n:foo' will find items with 'foolish' in the name)",
            "Tip:  An EQUALS (=) after a prefix  means  'EQUALS'; (eg 'n=foo' won't find items with 'foolish' in the name)",
            "Tip:  A BANG  (!)  before a prefix  means  'NOT' ",
            "Tip:  Search terms are NOT case sensitive (except keywords: AND & OR)",
            "Tip:  Search Confluence for 'Artisan Search Filter' for more search filter goodness",
            "Tip:  Search terms WITHOUT a prefix match on item name, owner, state and tags",

    ];
    static final String[] pearlsOfWisdom = [
            "The trouble with reality is, there's no background music",
            "Everyone is entitled to be stupid, but some abuse the privilege",
            "Life is like a coin. Spend it anyway you wish, but you can spend it only once",
            "Always remember you're unique... just like everyone else",
            "Knowledge speaks, but wisdom listens",
            "You never truly understand something until you can explain it to your grandmother",
            "Some people find fault like there was a reward (and in Artisan there is!)",
            "Change is inevitable, except from a vending machine",
            "Accept that some days you are the pigeon and some days the statue",
            "Don't drink and park - accidents cause people",
            "Solution to two of the world's problems: Feed the homeless to the hungry ?",
    ];
    int hintCounter = 0
    ULCTextField searchText
    final String hintText

    public TextFieldFocusListener(ULCTextField searchText, String hintText) {
        this.searchText = searchText;
        this.hintText = hintText
    }

    void focusGained(FocusEvent focusEvent) {
        if (isAHintInstalled()) {
            searchText.setText("")
            searchText.setForeground(Color.black)
        }
    }

    void focusLost(FocusEvent focusEvent) {
        String text = searchText.getText()
        if (!text || text.trim() == "") {
            installHint()
        }
    }

    // Evolve this so it alternates the standard hint with one of a list of tips
    //
    void installHint(){
        if( hintText.contains('comments') ){
            // nothing interesting to do - we're in the comment search field
            //
            searchText.setText(hintText)
            searchText.setForeground(Color.gray)
        }else{
            if( ++hintCounter > 19999 ){
                hintCounter=0
            }
            if( (hintCounter % 2) != 0){
                // Odds - Use supplied hint text
                //
                searchText.setText(hintText)
                searchText.setForeground(Color.gray)
            }else{
                // Evens - half the time, use alternative hints
                //
                if( rnd.nextInt(1000) < 800 ){
                    // Three quarters of the time we'll teach user about search filter
                    //
                    installHintFrom(searchFilterTips)
                    searchText.setForeground(Color.darkGray)
                }else{
                    // Occasional: entertain and/or enlighten
                    //
                    installHintFrom(pearlsOfWisdom)
                    searchText.setForeground(Color.lightGray)
                }
            }
        }
    }

    void installHintFrom(String[] list){
        int index = rnd.nextInt(list.length)
        try{
            searchText.setText(list[index])
        }catch(ArrayIndexOutOfBoundsException e){
            LOG.warn("OOPS random index generation broken", e)
            searchText.setText(hintText)
        }
    }

    boolean isAHintInstalled(){
        return StringUtils.equals(hintText, searchText.getText()) ||
               searchFilterTips.any { String it -> StringUtils.equals(it, searchText.getText()) } ||
               pearlsOfWisdom.any { String it -> StringUtils.equals(it, searchText.getText()) }
    }
}
