import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.google.gson.Gson;


public class Crawler {
	private static Gson gson = new Gson();
	private int depthLimit;
	Jedis jedis;
	Queue<MyUrl> links = new LinkedList<MyUrl>();
	
	
	public static void main(String[] args) {
		String orgUrl = args[0];
		int depth = Integer.parseInt(args[1]);
		Crawler mCrawler = new Crawler();
		mCrawler.startCrawl(orgUrl, depth, 100);
		System.out.println("finish!");
	}


	public void startCrawl(String url, int depth, int nitem_limit) {
		System.out.println("Craw: url=" + url + " depth: " + depth);
		jedis = new Jedis("localhost");
		depthLimit = depth;
		MyUrl currentUrl = new MyUrl(url, 0);
		PageData pageData;
		int nItem = 0;
		while (currentUrl != null && nItem < nitem_limit) {
			System.out.println("url=" + currentUrl.url + " depth="
					+ currentUrl.depth);
			pageData = getContent(currentUrl);
			storePageData(pageData);
			nItem++;
			if (pageData.getDepth() < depthLimit) {
				for (String link : pageData.getLinks()) {
					addLink(new MyUrl(link, pageData.getDepth() + 1));
				}
			}
			currentUrl = getNextLink();
		}
	}

	private void storePageData(PageData pageData) {
		// TODO Auto-generated method stub
		try {
			jedis.set(pageData.getUrl(), gson.toJson(pageData));
		} catch (JedisConnectionException e) {
			// TODO Auto-generated catch block
			System.out.println("Cannot connect to redis!");
			System.exit(0);
		}
	}

	private MyUrl getNextLink() {
		// TODO Auto-generated method stub
		MyUrl url = null;
		try {
			do {
				url = links.poll();
				if (url == null) {
					return url;
				}
			} while (jedis.exists(url.url));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e.toString());
			System.out.println("Cannot connect to redis!");
			System.exit(0);
		}
		return url;
	}

	public PageData getContent(MyUrl curUrl) {
		String url = curUrl.url;
		int depth = curUrl.depth;
		PageData data = new PageData();
		data.setUrl(url);
		data.setDepth(depth);
		ArrayList<String> links = new ArrayList<String>();
		try {
			Document doc = Jsoup.connect(url).get();
			Elements eTitles = doc.select("div#content > h1 > span");
			data.setTitle(eTitles.get(0).text());

			Elements eContents = doc.select("div#mw-content-text > p");
			data.setContent(eContents.get(0).text());

			Elements eLinks = doc.select("a[href~=/wiki/[^File]]");
			for (Element e : eLinks) {
				String link = e.attr("href");
				if (link.startsWith("/wiki/")) {
					link = "http://en.wikipedia.org" + link;
				}
				links.add(link);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Page " + url + "error when parsing content!");
			data.setContent("Content Format Error!");
		}
		data.setLinks(links);
		return data;
	}

	public void addLink(MyUrl url) {
		links.add(url);
	}
}
