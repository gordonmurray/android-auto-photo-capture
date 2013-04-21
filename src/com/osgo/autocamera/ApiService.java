package com.osgo.autocamera;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class ApiService {
	
	public static final String BROADCAST_ASSIGMENTS = "com.osgo.verifired.AssignmentList.syncassignments";
    public static final String BROADCAST_SURVEY= "com.osgo.verifired.Survey.syncsurvey";
    public static final String BROADCAST_SYNC= "com.osgo.verifired.sync";
    public static final String BUG_API = "c5db6163";
    
	private static final String DEBUG_TAG = null;
	private String API = "";
	public static final String SUBMIT_SURVEY = "com.osgo.verifired.Survey.submit";
	public static final String SAVE_SURVEY = "com.osgo.verifired.Survey.savedb";
	public static final String SUBMIT_IMAGE = "com.osgo.verifired.Survey.submitimage";
	private static String API_KEY;
	private Context context;
	
	public ApiService(String key, String api, Context context){
        API_KEY = key;
        API = api;
        this.context = context;
	}
	
	// Reads an InputStream and converts it to a String.
	public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
          builder.append(line);
        }
		return builder.toString();
	}
	
	private JSONObject sendPOST(String url, JSONObject input) throws ClientProtocolException, IOException, JSONException{
		int len = 500;
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost postRequest = new HttpPost(url);
 
		System.out.println(input.toString());
		StringEntity json = new StringEntity("data="+input.toString());
		postRequest.addHeader("content-type", "application/x-www-form-urlencoded");
		postRequest.setEntity(json);
	
		HttpResponse response = httpClient.execute(postRequest);
 
		if (response.getStatusLine().getStatusCode() != 200) {
			// Log error and issue toast to user
		}
 
		String responseJSON = readIt(response.getEntity().getContent(), len);
        try {
        	JSONObject jsonObj = new JSONObject(responseJSON);
        	httpClient.getConnectionManager().shutdown();
        	return jsonObj;
          } catch (Exception e) {
            e.printStackTrace();
          }
		
 
		httpClient.getConnectionManager().shutdown();
		
		return null;
	}
	
	public void submitMedia(Map<String, Object> input){
		
		String url = (String) input.get("URL");
		String path = (String) input.get("MEDIA");
		String name = (String) input.get("MEDIA_NAME");
		Long surveyId = (Long) input.get("SURVEY");		

		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(url);
		post.setEntity(readyMedia(path, name));
		
		HttpResponse response;
		try {
			response = client.execute(post);
			HttpEntity resEntity = response.getEntity();
	        String responseStr = EntityUtils.toString(resEntity);
	        JSONObject results = new JSONObject(responseStr);
	        
	        if(results!=null){
	        	if(results.get("result").equals("success")){
	        		
	            }
	        }
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private MultipartEntity readyMedia(String path, String name){

		
		FileBody fileBody = new FileBody(new File(path));
		MultipartEntity reqEntity = new MultipartEntity();
		try {	
			if(name.contains(".jpg")){
				reqEntity.addPart("photo", fileBody);
				reqEntity.addPart("image_type", new StringBody("image/jpeg"));
			} else if (name.contains(".mp4")){
				reqEntity.addPart("video", fileBody);
				reqEntity.addPart("video_type", new StringBody("video/mp4"));
			} 
			
			reqEntity.addPart("name", new StringBody(name));
		} catch (Exception e){
			
		}
    
        return reqEntity;		
	}
	
	private JSONObject sendRequest(String api, String method, JSONObject input) throws IOException{
		
		InputStream is = null;
	    // Only display the first 500 characters of the retrieved
	    // web page content.
	    int len = 500;
	    String result = "";
	    
	    if(method.equals("POST")){    	
            try {
            	JSONObject jsonObj = sendPOST(api, input);
				return jsonObj;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}   	
	    } else {
		    try {
		    	
		        URL url = new URL(api);
		        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		        conn.setReadTimeout(10000 /* milliseconds */);
		        conn.setConnectTimeout(15000 /* milliseconds */);
		        conn.setRequestMethod(method);
		        conn.setDoInput(true);
		        
		        if(input!=null){
		        	String data = input.toString();
		        	conn.setRequestProperty("Content-Type", "application/json");
		        	conn.connect();
		        	OutputStream os = conn.getOutputStream();
		        	os.write(data.getBytes());
		        	os.close();
		        } else {
			        // Starts the query
			        conn.connect();
		        }
		        int response = conn.getResponseCode();
		        Log.d(DEBUG_TAG, "The response is: " + response);
		        is = conn.getInputStream();
	
		        // Convert the InputStream into a string
		        String json = readIt(is, len);
		        try {
		        	JSONObject jsonObj = new JSONObject(json);
		        	result = (String) jsonObj.get("result");
		            if(result.contains("success")){
		            	return jsonObj;
		            } else {
		            	return jsonObj;
		            }
		          } catch (Exception e) {
		            e.printStackTrace();
		          }
		        
		    // Makes sure that the InputStream is closed after the app is
		    // finished using it.
		    } catch (Exception e) {
		    	e.printStackTrace();
		    } finally {
		        if (is != null) {
		            is.close();
		        } 
		    }
			
			return null;
	    }
		return null;
	}
	
	private List<String> commaSeperatedList(String list){
		List<String> arrayList = new ArrayList<String>();
		
		String string = "";
		int i = list.indexOf(",");
		if(i<0){
			string = list;
			arrayList.add(string);
			return arrayList;
		} else {
			string = list.substring(0, i);
			arrayList.add(string);
		}
		
		i++;
		try{
			while(true){
				String next = "";
				int k = list.indexOf(",", i);
				if(k==-1){
					next = list.substring(i);
					arrayList.add(next);
					break;
				} else {
					next = list.substring(i,k);
					arrayList.add(next);
					k++;
					i = k;
				}					
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return arrayList;
	}
	
}
