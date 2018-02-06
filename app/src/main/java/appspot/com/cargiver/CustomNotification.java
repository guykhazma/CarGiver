package appspot.com.cargiver;

import java.util.List;

/**
 * Created by Stav on 06/02/2018.
 */

public class CustomNotification {
    private String title;
    private String body;
    private List<String> tokens;

    public void setTitle(String title){
        this.title = title;
    }

    public void setBody(String body){
        this.body = body;
    }

    public void setTokens(List<String> tokens){
        this.tokens = tokens;
    }

    public String getTitle(){
        return this.title;
    }

    public String getBody(){
        return this.body;
    }

    public List<String> getTokens(){
        return this.tokens;
    }
}
