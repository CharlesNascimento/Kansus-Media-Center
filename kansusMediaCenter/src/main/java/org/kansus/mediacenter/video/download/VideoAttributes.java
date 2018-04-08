package org.kansus.mediacenter.video.download;

/**
 * Classe que representa as propriedades de um vídeo.
 * 
 * @author Charles
 */
public class VideoAttributes {

	private String title = "";
	private String duration = "";
	private String size = "";
	private String videoWidth = "";
	private String videoHeight = "";

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getVideoWidth() {
		return videoWidth;
	}

	public void setVideoWidth(String videoWidth) {
		this.videoWidth = videoWidth;
	}

	public String getVideoHeight() {
		return videoHeight;
	}

	public void setVideoHeight(String videoHeight) {
		this.videoHeight = videoHeight;
	}

	public String getResolution() {
		return this.videoWidth + " x " + this.videoHeight;
	}
}
