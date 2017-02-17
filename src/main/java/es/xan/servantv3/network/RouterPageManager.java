package es.xan.servantv3.network;

import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class RouterPageManager {
	
	private JsonObject mConfiguration;
	
	private static final String LOGIN_URI = "/cgi-bin/login.exe";
	private static final String DEVICES_URI = "/pc_list_view.stm";

	public RouterPageManager(JsonObject configuration) {
		this.mConfiguration = configuration;
	}

	
	public List<Device> getDevices() throws ClientProtocolException, IOException {
		String server = this.mConfiguration.getString("server");
		String usr = this.mConfiguration.getString("usr");
		String pws = this.mConfiguration.getString("pws");
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(server + LOGIN_URI);
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("usr", usr));
		nvps.add(new BasicNameValuePair("pws", pws));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		
		HttpContext localContext = new BasicHttpContext();
		CookieStore cookieStore = new BasicCookieStore();
		localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

		httpclient.execute(httpPost, localContext);

		HttpGet httpGet = new HttpGet(server + DEVICES_URI);
		CloseableHttpResponse execute = httpclient.execute(httpGet, localContext);
		
		String content = IOUtils.toString(execute.getEntity().getContent(), "UTF-8");
		
		List<Device> result = parseAndExtractDevices(content);
		
		return result;
		
	}


	private List<Device> parseAndExtractDevices(String content)
			throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(content));
		String line = null;
		

		List<Device> result = new ArrayList<>();
		Pattern pattern = Pattern.compile(".*\"(.*)\";$");
		Device item = null;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("pc_list")) {
				if (line.contains("PC_LIST_ENTRY")) {
					item = new Device();
				} else if (line.contains("].hostname = \"")) {
					Matcher matcher = pattern.matcher(line);
					
					if (matcher.matches()) {
						item.name = matcher.group(1);
					}

				} else if (line.contains("].mac[")) {
					Matcher matcher = pattern.matcher(line);
					
					if (matcher.matches()) {
						if (item.mac == null) {
							item.mac = matcher.group(1);
						} else {
							item.mac += ":" + matcher.group(1);
						}
					}
				} 
				else if (line.contains("].active = ")) {
					if (line.endsWith("1;")) {
						item.active = true;
					} else {
						item.active = false;
					}
				} else if (line.contains("].ip")) {
					Matcher matcher = pattern.matcher(line);
					
					if (matcher.matches()) {
						if (item.ip == null) {
							item.ip = matcher.group(1);
						} else {
							item.ip += "." + matcher.group(1);
						}
					}
				}
				
				else if (line.contains("].index =")) {
					if (item.active) {
//						System.out.println(item.name + " " + item.ip + " " + item.mac);
						result.add(item);
					}
				}
			}
		}
		return result;
	}
	
	public static class Device {
		public String ip;
		public String mac;
		public String name;
		public boolean active;
	}
}
