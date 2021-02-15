package com.heroku;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.nyatla.minya.SingleMiningChief;
import jp.nyatla.minya.connection.IMiningConnection;
import jp.nyatla.minya.connection.StratumMiningConnection;
import jp.nyatla.minya.worker.CpuMiningWorker;
import jp.nyatla.minya.worker.IMiningWorker;

@Controller
@EnableAutoConfiguration
@SpringBootApplication
public class HerokuCoinApplication {
	SingleMiningChief worker;
	int stop = 0;
	int mining = 0;

	@RequestMapping("/")
	public String index() {
		return "index";
	}

	@RequestMapping("/stop")
	public String stop(@RequestParam(name = "list") String list, Model model) throws Exception {
		stop = 1;
		String zero = list.split(" ")[0];
		String[] after = removeTheElement(list.split(" "), 0);
		Thread thread = new Thread() {
			public void run() {
				try {
					URL siteURL = new URL("http://" + zero + "/stop?list=" + String.join("%20", after));
					HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
					connection.setRequestMethod("GET");
					connection.connect();
					connection.getResponseCode();
				} catch (Exception e) {
				}
			}
		};
		thread.start();
		try {
			worker.stopMining();
		} catch (Exception e) {
		}
		
		return "index";
	}

	@RequestMapping("/start")
	public String start(@RequestParam(name = "list") String list, @RequestParam(name = "url") String url,
			@RequestParam("name") String name, @RequestParam("pass") String pass, Model model) throws Exception {

		String zero = list.split(" ")[0];
		String[] after = removeTheElement(list.split(" "), 0);
		
		if (mining == 0) {
			mining = 1;
			
			Thread threadPing = new Thread() {
				String[] after2 = after;
				public void run() {
					while (stop == 0) {
						try {
							String Ping = "http://" + after2[0] + "/start?list=" + String.join("%20", after2) + "&url="
									+ URLEncoder.encode(url, "UTF-8") + "&name=" + name + "&pass=" + pass;
							URL siteURL = new URL(Ping);
							HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
							connection.setRequestMethod("GET");
							connection.connect();
							int code = connection.getResponseCode();
							if (200 != code) {
								after2 = removeTheElement(after2, 0);
							} else {
								TimeUnit.MINUTES.sleep(25);
							}
							
						} catch (Exception e) {
						}
					}
				}
			};
			threadPing.start();
			
			try {
				int cpuProcessors = Runtime.getRuntime().availableProcessors();
				IMiningConnection mc = new StratumMiningConnection(url, name, pass + " n=" + zero);
				IMiningWorker imw = new CpuMiningWorker(cpuProcessors);
				worker = new SingleMiningChief(mc, imw);
				worker.startMining();
			} catch (Exception e) {
			}
		}

		return "index";
	}

	public static String[] removeTheElement(String[] arr, int index) {

		if (arr == null || index < 0 || index >= arr.length) {

			return arr;
		}
		String[] anotherArray = new String[arr.length - 1];
		for (int i = 0, k = 0; i < arr.length; i++) {
			if (i == index) {
				continue;
			}
			anotherArray[k++] = arr[i];
		}
		return anotherArray;
	}

	public static void main(String[] args) {
		SpringApplication.run(HerokuCoinApplication.class, args);
	}
}
