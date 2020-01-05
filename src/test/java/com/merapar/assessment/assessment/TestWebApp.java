package com.merapar.assessment.assessment;

import java.nio.charset.Charset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.http.MediaType;


public class TestWebApp extends AssessmentApplicationTests {
	
	public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));


	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testAnalyzer() throws Exception {
	    mockMvc.perform(post("/analyze").contentType(APPLICATION_JSON_UTF8)
	            .content("{\"url\": \"https://s3-eu-west-1.amazonaws.com/merapar-assessment/3dprinting-posts.xml\"}"))
	    		.andExpect(jsonPath("$.details.totalPosts").value("655"))
	    		.andExpect(jsonPath("$.details.avgScore").value("3"))
	    		.andExpect(jsonPath("$.details.totalAcceptedPosts").value("102"))
	    		.andExpect(jsonPath("$.details.lastPost").value("2016-03-04T13:30:22.410"))
	    		.andExpect(jsonPath("$.details.firstPost").value("2016-01-12T18:45:19.963"))
	    		.andExpect(status().isOk());
	}

}