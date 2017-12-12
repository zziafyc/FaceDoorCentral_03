package com.example.facedoor.util;

import com.faceplusplus.api.FaceDetecter;

import android.content.Context;

public class FaceppUtility {
	private static FaceDetecter faceDetector;
	
	public static void createFaceDetector(Context context, String apiKey){
		if(faceDetector == null){
			faceDetector = new FaceDetecter();
			faceDetector.init(context, "ef9ff4741e6a5964adb15856d13ed854");
		}
	}
	
	public static FaceDetecter getFaceDetector(){
		return faceDetector;
	}
}
