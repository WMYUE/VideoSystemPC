package com.jd.Event;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.jd.model.FileInfo;
import com.jd.sdk.LetvCloudV1;
import com.jd.window.MainFrame;
import com.jd.window.MainPanel;

public class UploadThread extends Thread{
	
	public String USER_UNIQUE = "ievel39qfn";
	public String SECRET_KEY = "400c0826066c64f8a3b8c64d55342ea1";
    public LetvCloudV1 client = null;
    public boolean isStop = true;
    
	/*
	 * 
	 */
	public UploadThread()
	{
		client = new LetvCloudV1(USER_UNIQUE, SECRET_KEY);
		client.format = "json";
		
		uploadInit(MainFrame.currentUploadFile);
	}
	
	/*
	 * 
	 */
	public void uploadInit(int index)
	{
		FileInfo fileInfo = MainFrame.fileinfoVector.get(index);
		String response;
		try {
			response = client.videoUploadInit(fileInfo.file.getName());
			//
			List<Map<String, String>> responseList = jsonStringToList(response);
			Map<String, String> map = (Map<String, String>)responseList.get(0);
			//
			if(map.get("code").equals("0"))
			{
				List<Map<String, String>> dataList = jsonStringToList(map.get("data"));
				MainFrame.fileinfoVector.get(index).fileStatus = "正在上传";
				MainFrame.fileinfoVector.get(index).upload_url = dataList.get(0).get("upload_url");
				MainFrame.fileinfoVector.get(index).progress_url = dataList.get(0).get("progress_url");
				MainFrame.fileinfoVector.get(index).video_id = dataList.get(0).get("video_id");
				MainFrame.fileinfoVector.get(index).video_unique = dataList.get(0).get("video_unique");
				MainFrame.fileinfoVector.get(index).token = dataList.get(0).get("token");
				
				MainPanel.ftp.init();
				MainFrame.mainPanel.validate();
				//
				if(!this.isAlive())
				{
					this.start();
				}
				upload(index);
			}
			else{  //
				uploadInit(index);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 */
	public void upload(int index)
	{
		String response2;
		try {
			response2 = client.videoUpload(MainFrame.fileinfoVector.get(index).file.getAbsolutePath(),
					MainFrame.fileinfoVector.get(index).upload_url);
			
			List<Map<String, String>> responseList = jsonStringToList(response2);
			//
			if(responseList.get(0).get("code").equals("0"))
			{
				//
				System.out.println("完成一个");
				MainFrame.fileinfoVector.get(MainFrame.currentUploadFile).fileStatus = "上传完成";
				MainFrame.fileinfoVector.get(MainFrame.currentUploadFile).prodrass = 100;
				
				if(index < MainFrame.fileinfoVector.size()-1)
				{
					MainFrame.currentUploadFile++;
					uploadInit(MainFrame.currentUploadFile);
				}
				else{
					System.out.println("全部上传完成");
					this.stop();
					
					MainPanel.ftp.init();
					MainFrame.mainPanel.validate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 */
	public List<Map<String, String>> jsonStringToList(String rsContent) throws Exception
    {
        JSONArray arry = JSONArray.fromObject("["+rsContent+"]");

        List<Map<String, String>> rsList = new ArrayList<Map<String, String>>();
        for (int i = 0; i < arry.size(); i++)
        {
            JSONObject jsonObject = arry.getJSONObject(i);
            Map<String, String> map = new HashMap<String, String>();
            for (Iterator<?> iter = jsonObject.keys(); iter.hasNext();)
            {
                String key = (String) iter.next();
                String value = jsonObject.get(key).toString();
                map.put(key, value);
            }
            rsList.add(map);
        }
        return rsList;
    }
	
	
	@Override
	public void run() {
		
		while(true)
		{
			try {
				//
				String res = client.doGet(MainFrame.fileinfoVector.get(MainFrame.currentUploadFile).progress_url);
				//System.out.println(MainFrame.fileinfoVector.get(MainFrame.currentUploadFile).progress_url);
				List<Map<String, String>> prograssList = jsonStringToList(res);
				if(prograssList.get(0).get("code").equals("0")
						&&prograssList.get(0).get("data").length()!=0)
				{
					List<Map<String, String>> dataList = jsonStringToList(prograssList.get(0).get("data"));
					int upload_size = Integer.valueOf(dataList.get(0).get("upload_size"));
					int total_size = Integer.valueOf(dataList.get(0).get("total_size"));
					
					MainFrame.fileinfoVector.get(MainFrame.currentUploadFile).prodrass = (upload_size/(total_size*1.0+0.01))*100;
					
					MainPanel.ftp.init();
					MainFrame.mainPanel.validate();
				}
				
				Thread.sleep(40);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}catch (IllegalArgumentException e2) {
				e2.printStackTrace();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
