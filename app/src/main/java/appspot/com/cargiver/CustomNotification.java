package appspot.com.cargiver;

/**
 * Created by Stav on 06/02/2018.
 */

public class CustomNotification {
    private String title;
    private String body;
    private String[] tokens;

    public void setTitle(String title){
        this.title = title;
    }

    public void setBody(String body){
        this.body = body;
    }

    public void setTokens(String[] tokens){
        this.tokens = tokens;
    }

    public String getTitle(){
        return this.title;
    }

    public String getBody(){
        return this.body;
    }

    public String[] getTokens(){
        return this.tokens;
    }
}
