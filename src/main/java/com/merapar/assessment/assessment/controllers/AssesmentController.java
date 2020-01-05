package com.merapar.assessment.assessment.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.codehaus.jettison.json.JSONObject;

@RestController
public class AssesmentController {
	
	@PostMapping("/analyze")
	public Response Analyze(@RequestBody String json) {
		
		JSONObject obj;
				
		try {
			obj = new JSONObject(json);
			String url = obj.getString("url");
			
			Analyzer analyzer = new Analyzer(url);
			analyzer.parseFile();
			
			return analyzer.getResult();
			
		} catch (Exception ex) {
			ex.printStackTrace();
			return new Error(ex);
		}
	}
	
	private class Analyzer {
		private String url;
		private String filePath;
		private int totalPost;
		private int totalScore;
		private int acceptedPosts;
		private String firstPost;
		private String lastPost;
		private int avgScore;
		
		public Analyzer(String url) {
			super();
			this.url = url;
			
			this.downloadFile();
		}
		
		AnalyzeOutput getResult() {
			return new AnalyzeOutput(
					this.acceptedPosts,
					this.avgScore,
					this.totalPost,
					this.firstPost,
					this.lastPost
    		); 
		}
		
		void parseFile() throws IOException, SAXException, ParserConfigurationException, DOMException, ParseException {
			FileInputStream inputStream = null;
			Scanner sc = null;
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			try {
			    inputStream = new FileInputStream(this.filePath);
			    sc = new Scanner(inputStream, "UTF-8");
			    while (sc.hasNextLine()) {
			        String line = sc.nextLine();
			        if(line.contains("<row")) {
			        	parseLine(line, factory, builder);
					}
			    }
			    // note that Scanner suppresses exceptions
			    if (sc.ioException() != null) {
			        throw sc.ioException();
			    }
			} finally {
			    if (inputStream != null) {
			        inputStream.close();
			    }
			    if (sc != null) {
			        sc.close();
			    }
			    
			    this.avgScore = Math.abs(this.totalScore / this.totalPost);
			}
		}
		
		void parseLine(String line, DocumentBuilderFactory factory, DocumentBuilder builder) throws SAXException, IOException, DOMException, ParseException {
			try {
				builder = factory.newDocumentBuilder();
	            Document document = builder.parse(new InputSource(new StringReader(line)));
				NodeList nodes = document.getElementsByTagName("row");
				
				this.totalPost += 1;
				this.totalScore += Integer.parseInt(nodes.item(0).getAttributes().getNamedItem("Score").getNodeValue());
				
				if(nodes.item(0).getAttributes().getNamedItem("AcceptedAnswerId") != null) {
					this.acceptedPosts += 1;
				}
				
				this.firstPost = dateCompare(nodes.item(0).getAttributes().getNamedItem("CreationDate").getNodeValue(), this.firstPost, true);
				this.lastPost = dateCompare(nodes.item(0).getAttributes().getNamedItem("CreationDate").getNodeValue(), this.lastPost, false);
				
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}  
		}
		
		void downloadFile() {
			Path currentRelativePath = Paths.get("");
			String relativePath = currentRelativePath.toAbsolutePath().toString();
			
			try (BufferedInputStream in = new BufferedInputStream(new URL(this.url).openStream());
			  FileOutputStream fileOutputStream = new FileOutputStream(relativePath +"analyze.xml")) {
			    byte dataBuffer[] = new byte[1024];
			    int bytesRead;
			    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
			        fileOutputStream.write(dataBuffer, 0, bytesRead);
			    }
			    
			    this.filePath = relativePath + "analyze.xml";
			} catch (IOException e) {
				this.filePath = null;
			}
		}
	}
	
	private String dateCompare(String attributeDate, String currentDate, Boolean newest) throws ParseException {
		if(currentDate == null) return attributeDate;
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        
		LocalDateTime tempAttDate = LocalDateTime.parse(attributeDate, formatter);
		LocalDateTime tempCurrDate = LocalDateTime.parse(currentDate, formatter);  
		
		if (tempAttDate.isAfter(tempCurrDate) && newest == true || tempCurrDate.isAfter(tempAttDate) && newest == false) {
			return currentDate;
		}
		return attributeDate;
	}
	
	private interface Response {}
	
	private class Error implements Response {
		private Exception ex;

		public Error(Exception ex) {
			super();
			this.ex = ex;
		}

		public Exception getEx() {
			return ex;
		}

		public void setEx(Exception ex) {
			this.ex = ex;
		}
	}
	
	private class AnalyzeOutput implements Response {
		private String analyseDate;
		private HashMap <String, Object> details;
		
	   public AnalyzeOutput(int totalAcceptedPosts, int avgScore, int totalPosts, String firstPost, String lastPost) {
	      this.analyseDate = new Date().toString();
	      
          this.details = new HashMap<String, Object>();
          this.details.put("totalAcceptedPosts", totalAcceptedPosts);
          this.details.put("avgScore", avgScore);
          this.details.put("totalPosts", totalPosts);
          this.details.put("firstPost", firstPost);
          this.details.put("lastPost", lastPost);
	   }
	   
		public HashMap<String, Object> getDetails() {
			return details;
		}

		public void setDetails(HashMap<String, Object> details) {
			this.details = details;
		}

		public String getAnalyseDate() {
			return analyseDate;
		}

		public void setAnalyseDate(String analyseDate) {
			this.analyseDate = analyseDate;
		}
	}
}