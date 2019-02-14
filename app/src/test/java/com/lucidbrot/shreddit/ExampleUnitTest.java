package com.lucidbrot.shreddit;

import android.util.Log;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
	@Test
	public void addition_isCorrect() {
		assertEquals(4, 2 + 2);
	}

	@Test
	public void htmlRegex_isGood(){

		String html = "\n" +
				"<!DOCTYPE html><html lang=\"en\"><head><script>\n"+
				"https://external-preview.redd.it/kmeBt8R6jt4X-Zhio7K8BbzvHhvowyxYy2IrjaVpBMU.jpg?auto=webp&amp;" +
				"s=17c5a4da7d2d691e376cfee07c6cb17e6716f6ea\"/>asdf/>"+
				                                                    ".redditstatic.com/desktop2x/CommentsPage.927f6ef3e63b6afede57" +
				                                                     ".js\"></script></body></html>";

		// https://external-preview.redd.it/kmeBt8R6jt4X-Zhio7K8BbzvHhvowyxYy2IrjaVpBMU.jpg?auto=webp&amp;s=17c5a4da7d2d691e376cfee07c6cb17e6716f6ea"/>
		//Pattern pattern = Pattern.compile("https://external-preview.redd.it/(.*?)/");
		Pattern pattern = Pattern.compile("https://external\\-preview\\.redd\\.it(.*?)\"");
		Matcher matcher = pattern.matcher(html);
		String url = null;
		if(matcher.find()){
			url = matcher.group(0);
		} else {

		}
		assertNotEquals(url, null);
	}
}