package org.kansus.mediacenter.video.download;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * Classe que auxilia o download de vídeos do youtube.
 * @author Charles
 */
public class YouTubeDownload {

	/**
	 * Listener de eventos ocorridos durante o download de um vídeo.
	 * 
	 * @author Charles
	 */
	public interface IDownloadListener {

		/**
		 * Método acionado quando os atributos do vídeo terminam de ser
		 * coletados.
		 * 
		 * @param videoAttributes
		 *            atributos do vídeo.
		 */
		public void onCompletedGettingVideoAttributes(
				VideoAttributes videoAttributes);

		/**
		 * Método acionado quando a porcentagem do download aumenta.
		 * 
		 * @param percentage
		 *            porcentagem completada.
		 */
		public void onPercentageChange(int percentage);

		/**
		 * Método acionado quando um download é completado com sucesso.
		 */
		public void onDownloadComplete();

		/**
		 * Método acionado quando um download é cancelado.
		 */
		public void onDownloadCancel();

		/**
		 * Método acionado quando há um erro de conexão.
		 */
		public void onConnectionError();
	}

	private static final String YOUTUBE_HOST_REGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?(.*)\\.(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/";

	private static final int FLV_240P = 5;
	private static final int FLV_270P = 6;
	private static final int _3GP_144P = 17;
	private static final int MP4_360P = 18;
	private static final int MP4_720P = 22;
	private static final int FLV_360P = 34;
	private static final int FLV_480P = 35;
	private static final int _3GP_240P = 36;
	private static final int MP4_1080P = 37;
	private static final int MP4_3072P = 38;
	private static final int WEBM_360P = 43;
	private static final int WEBM_480P = 44;
	private static final int WEBM_720P = 45;
	private static final int WEBM_1080P = 46;

	// Listener de eventos
	private IDownloadListener listener;

	// <Formato do vídeo, Link de download>
	private HashMap<String, String> videoUrls;

	// Propriedades do vídeo
	private VideoAttributes videoAttributes = new VideoAttributes();

	// Auxiliares
	private boolean downloadCancelado = false;
	private boolean interrompido = false;

	// Leitores de stream
	private BufferedReader textReader = null;
	private BufferedInputStream binaryReader = null;

	// Conexão
	private HttpGet httpGet = null;
	private HttpClient httpClient = null;
	private HttpHost target = null;
	private HttpContext localContext = null;
	private HttpResponse response = null;
	private String contentType = null;

	public YouTubeDownload(IDownloadListener listener) {
		this.listener = listener;
	}

	/**
	 * Efetua o download do vídeo. Só chame este método depois que obtiver os
	 * formatos disponíveis para download.
	 * 
	 * @param url
	 *            link de download do vídeo.
	 * @param outputDir
	 *            Diretório onde o vídeo deve ser salvo.
	 * @return true se o processo foi completado com sucesso.
	 */
	public boolean download(String url, String outputDir) {
		if (!executeRequest(url)) {
			this.httpClient.getConnectionManager().shutdown();
			listener.onConnectionError();
			return false;
		}

		HttpEntity entity = null;
		try {
			entity = this.response.getEntity();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}

		if (entity != null) {
			try {
				this.binaryReader = new BufferedInputStream(entity.getContent());
			} catch (IllegalStateException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				this.contentType = this.response.getFirstHeader("Content-Type")
						.getValue().toLowerCase();
				if (this.contentType.matches("video/(.)*")) {
					saveBinaryData(outputDir);
				} else {
					return false;
				}
			} catch (IOException ex) {
				try {
					throw ex;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (RuntimeException ex) {
				try {
					throw ex;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		this.httpClient.getConnectionManager().shutdown();

		if (!downloadCancelado) {
			this.listener.onDownloadComplete();
			System.out.println("Download completado.");
			return true;
		} else {
			this.listener.onDownloadCancel();
			System.out.println("Download cancelado.");
			return false;
		}
	}

	/**
	 * Configura a conexão com a página do vídeo.
	 * 
	 * @param url
	 *            URL da página.
	 */
	private void configureConnection(String url) {
		try {
			this.httpClient = new DefaultHttpClient();
			this.httpClient.getParams().setParameter(
					ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);
			this.httpGet = new HttpGet(getURI(url));
			if (url.toLowerCase().startsWith("https"))
				this.target = new HttpHost(getHost(url), 443, "https");
			else
				this.target = new HttpHost(getHost(url), 80, "http");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Executa uma requisição à URL.
	 * 
	 * @param url
	 *            URL.
	 * @return true se o processo foi completado com sucesso.
	 */
	public boolean executeRequest(String url) {
		if (url == null) {
			return false;
		}
		boolean rc200 = false;
		boolean rc204 = false;
		boolean rc302 = false;

		configureConnection(url);

		try {
			this.response = this.httpClient.execute(this.target, this.httpGet,
					this.localContext);
		} catch (ClientProtocolException cpe) {
			cpe.printStackTrace();
			return false;
		} catch (UnknownHostException uhe) {
			uhe.printStackTrace();
			return false;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		} catch (IllegalStateException ise) {
			ise.printStackTrace();
			return false;
		}

		try {
			System.out.println("HTTP response status line:"
					.concat(this.response.getStatusLine().toString()));

			if (!(rc200 = this.response.getStatusLine().toString()
					.toLowerCase().matches("^(http)(.*)200(.*)"))
					& !(rc204 = this.response.getStatusLine().toString()
							.toLowerCase().matches("^(http)(.*)204(.*)"))
					& !(rc302 = this.response.getStatusLine().toString()
							.toLowerCase().matches("^(http)(.*)302(.*)"))) {
				return false;
			}
			if (rc204) {
				return rc200;
			}
			if (rc302) {
				System.out.println("location from HTTP Header: "
						.concat(this.response.getFirstHeader("Location")
								.toString()));
			}

		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		return rc200;
	}

	/**
	 * Salva o vídeo em um arquivo.
	 * 
	 * @param dir
	 *            diretório onde o vídeo deve ser salvo.
	 * @throws IOException
	 */
	private void saveBinaryData(String dir) throws IOException {
		FileOutputStream fos = null;
		downloadCancelado = false;
		try {
			File f;
			int currentRenameTry = 0;
			String fileName = videoAttributes.getTitle();
			do {
				f = new File(dir, fileName
						.concat((currentRenameTry > 0 ? "(".concat(
								String.valueOf(currentRenameTry)).concat(")")
								: ""))
						.concat(".")
						.concat(this.contentType.replaceFirst("video/", "")
								.replaceAll("x-", "")));
				currentRenameTry += 1;
				if (f.getName() == "")
					fileName = "Downloaded Video";
			} while (f.exists() || f.getName() == "");

			long bytesReadSum = 0;
			long percentage = -1;
			long bytesMax = Long.parseLong(this.response.getFirstHeader(
					"Content-Length").getValue());
			fos = new FileOutputStream(f);

			System.out.println(String.format("writing %d bytes to: %s",
					bytesMax, f.getName()));

			byte[] bytes = new byte[4096];
			int bytesRead = 1;

			while (bytesRead > 0 && !downloadCancelado) {
				bytesRead = this.binaryReader.read(bytes);
				bytesReadSum += bytesRead;
				if ((bytesReadSum * 100 / bytesMax) > percentage) {
					percentage = bytesReadSum * 100 / bytesMax;
					this.listener.onPercentageChange(Integer.parseInt(String
							.valueOf(percentage)));
				}

				try {
					fos.write(bytes, 0, bytesRead);
				} catch (IndexOutOfBoundsException ioob) {
				}
			}

			if (++bytesReadSum < bytesMax || downloadCancelado) {
				try {
					fos.close();
				} catch (Exception e) {
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				downloadCancelado = true;
				this.httpClient.getConnectionManager().shutdown();
			}
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			throw (fnfe);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw (ioe);
		} finally {
			try {
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				this.binaryReader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Pega todos os formatos dísponíveis deste vídeo para download.
	 * 
	 * @param url
	 *            URL do vídeo.
	 * @return true se o processo foi completado com sucesso.
	 */
	public boolean getAvaliableFormats(String url) {
		if (!executeRequest(url)) {
			this.httpClient.getConnectionManager().shutdown();
			listener.onConnectionError();
			return false;
		}

		HttpEntity entity = null;
		try {
			entity = this.response.getEntity();
		} catch (NullPointerException npe) {
		}

		if (entity != null) {
			try {
				if (this.response.getFirstHeader("Content-Type").getValue()
						.toLowerCase().matches("^text/html(.*)")) {
					this.textReader = new BufferedReader(new InputStreamReader(
							entity.getContent()));
					gatherVideoUrls();
				}
			} catch (IllegalStateException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		this.httpClient.getConnectionManager().shutdown();
		return true;
	}

	/**
	 * Lê todo o código HTML da página do vídeo, pegando os links de todos os
	 * formatos disponíveis para download, além de pegar outras informações
	 * importantes do vídeo.
	 * 
	 * @throws IOException
	 */
	private void gatherVideoUrls() throws IOException {
		interrompido = false;
		String line = this.textReader.readLine();
		while (line != null && !interrompido) {
			try {
				if (line.matches("(.*)generate_204(.*)")) {
					line = line.replaceFirst("img.src = '?", "");
					line = line.replaceFirst("';", "");
					line = line.replaceFirst("\\u0026", "&");
					line = line.replaceAll("\\\\", "");
					line = line.replaceAll("\\s", "");
					System.out.println("img.src URL: ".concat(line));
				} else if (line
						.matches("(.*)\"url_encoded_fmt_stream_map\":(.*)")) {
					videoUrls = new HashMap<String, String>();
					line = line
							.replaceFirst(
									".*\"url_encoded_fmt_stream_map\": \"", "")
							.replaceFirst("\".*", "").replace("%25", "%")
							.replace("\\u0026", "&").replace("\\", "");
					String[] urlStrings = line.split(",");
					for (String urlString : urlStrings) {
						String[] fmtUrlPair = urlString.split("&url=");
						fmtUrlPair[1] = fmtUrlPair[1].replaceFirst(
								"http%3A%2F%2F", "http://");
						fmtUrlPair[1] = fmtUrlPair[1].replaceAll("%3F", "?")
								.replaceAll("%2F", "/").replaceAll("%3D", "=")
								.replaceAll("%26", "&").replaceAll("%2C", ",")
								.replaceAll("%3B", ";")
								.replaceAll("sig", "signature");
						fmtUrlPair[1] = fmtUrlPair[1].replaceFirst(
								"&quality=.*", "");
						fmtUrlPair[0] = fmtUrlPair[0].replace("itag=", "");
						try {
							videoUrls.put(fmtUrlPair[0], fmtUrlPair[1]);
							System.out
									.println(String.format(
											"video url saved with key %s: %s",
											fmtUrlPair[0],
											videoUrls.get(fmtUrlPair[0])));
						} catch (java.lang.ArrayIndexOutOfBoundsException aioobe) {
							aioobe.printStackTrace();
						}
					}
				} else

				// Título
				if (line.matches("(.*)<meta name=\"title\" content=(.*)")) {
					videoAttributes
							.setTitle(line
									.replaceFirst(
											"<meta name=\"title\" content=", "")
									.trim()
									.replaceAll("&amp;", "&")
									.replaceAll(
											"[!\"#$%'*+,/:;<=>\\?@\\[\\]\\^`\\{|\\}~\\.]",
											""));
				} else
				// Duração
				if (line.matches("(.*)<meta itemprop=\"duration\" content=(.*)")) {
					String duration = line
							.replaceFirst(
									"<meta itemprop=\"duration\" content=", "")
							.trim()
							.replaceAll("&amp;", "&")
							.replaceAll(
									"[!\"#$%'*+,/:;<=>\\?@\\[\\]\\^`\\{|\\}~\\.]",
									"");
					setDuration(duration);
				} else
				// Largura do vídeo
				if (line.matches("(.*)<meta itemprop=\"width\" content=(.*)")) {
					videoAttributes
							.setVideoWidth(line
									.replaceFirst(
											"<meta itemprop=\"width\" content=",
											"")
									.trim()
									.replaceAll("&amp;", "&")
									.replaceAll(
											"[!\"#$%'*+,/:;<=>\\?@\\[\\]\\^`\\{|\\}~\\.]",
											""));
				} else
				// Altura do vídeo
				if (line.matches("(.*)<meta itemprop=\"height\" content=(.*)")) {
					videoAttributes
							.setVideoHeight(line
									.replaceFirst(
											"<meta itemprop=\"height\" content=",
											"")
									.trim()
									.replaceAll("&amp;", "&")
									.replaceAll(
											"[!\"#$%'*+,/:;<=>\\?@\\[\\]\\^`\\{|\\}~\\.]",
											""));
				}
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			}
			line = this.textReader.readLine();
		}
		try {
			this.textReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.listener.onCompletedGettingVideoAttributes(videoAttributes);
	}

	/**
	 * Pega o URI da uma URL do Youtube.
	 * 
	 * @param url
	 *            Url do youtube.
	 * @return Uri.
	 */
	private String getURI(String url) {
		String uri = "/"
				+ url.replaceFirst(YOUTUBE_HOST_REGEX, "").replaceFirst(
						"http[s]?://", "");
		return uri;
	}

	/**
	 * Pega o host de uma dada URL.
	 * 
	 * @param url
	 *            URL.
	 * @return host.
	 */
	String getHost(String url) {
		String host = url.replaceFirst(YOUTUBE_HOST_REGEX, "");
		host = url.substring(0, url.length() - host.length());
		host = host.toLowerCase().replaceFirst("http[s]?://", "")
				.replaceAll("/", "");
		return host;
	}

	/**
	 * Converte uma URL do Youtube, afim de que esta fique no formato
	 * "http://www.youtube.com/watch?v=00000000000".
	 * 
	 * @param input
	 *            url a ser processada.
	 * @return URL processada.
	 */
	public static String processYouTubeUrl(String input) {
		String pattern = "(?:videos\\/|v=)([\\w-]+)";

		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(input);

		if (matcher.find()) {
			if (matcher.group().length() < 12)
				return null;
			String finalURL = "http://www.youtube.com/watch?" + matcher.group();
			return finalURL;
		}
		return null;
	}

	/**
	 * Converte a duração do vídeo pego do código html(PT5M26S) para o formato
	 * padrão(5:26).
	 * 
	 * @param youTubeDuration
	 *            duração do vídeo pego do código HTML.
	 */
	private void setDuration(String youTubeDuration) {
		String duration = "";
		String segundos = "";
		String minutos = "";
		String horas = "";

		duration = youTubeDuration.substring(2);

		// get minutes
		int i = 0;
		for (char c : duration.toCharArray()) {
			i++;
			if (Character.isDigit(c)) {
				minutos += c;
			} else {
				break;
			}
		}
		duration = duration.substring(i);

		// get seconds
		for (char c : duration.toCharArray()) {
			if (Character.isDigit(c)) {
				segundos += c;
			} else {
				break;
			}
		}

		int minutosInt = Integer.parseInt(minutos);
		int segundosInt = Integer.parseInt(segundos);
		int horasInt;
		horasInt = minutosInt / 60;
		minutosInt -= (horasInt % 60) * 60;

		if (horasInt > 0) {
			horas = horasInt < 10 ? ("0" + horasInt) : "" + horasInt;
			minutos = minutosInt < 10 ? ("0" + minutosInt) : ("" + minutosInt);
		} else
			minutos = String.valueOf(minutosInt);
		segundos = segundosInt < 10 ? ("0" + segundosInt) : ("" + segundosInt);

		if (horasInt > 0)
			videoAttributes.setDuration(horas + ":" + minutos + ":" + segundos);
		else
			videoAttributes.setDuration(minutos + ":" + segundos);
	}

	/**
	 * Pega o tamanho, em Megabytes, de um vídeo em um determinado formato.
	 * 
	 * @param url
	 *            URL de download do vídeo.
	 */
	public void setVideoSize(String url) {
		if (!executeRequest(url)) {
			this.httpClient.getConnectionManager().shutdown();
			listener.onConnectionError();
			return;
		}

		HttpEntity entity = null;
		try {
			entity = this.response.getEntity();
		} catch (NullPointerException npe) {
		}

		if (entity != null) {
			double size = Double.parseDouble(this.response.getFirstHeader(
					"Content-Length").getValue());
			double sizeMb = size / 1024 / 1024;
			NumberFormat formatter = new DecimalFormat("0.00");
			System.out.println("Size: " + sizeMb);
			videoAttributes.setSize(formatter.format(sizeMb));
		}

		this.httpClient.getConnectionManager().shutdown();
	}

	/**
	 * Constoi um array de strings com os nomes do formatos disponíveis para
	 * download.
	 * 
	 * @return um <code>ArrayList<String></code> com os nomes do formatos
	 *         disponíveis.
	 */
	public ArrayList<String> avaliableFormatsToString() {
		ArrayList<String> avaliableFormatsString = new ArrayList<String>();
		for (String format : videoUrls.keySet()) {
			avaliableFormatsString
					.add(getFormatString(Integer.parseInt(format)));
		}
		return avaliableFormatsString;
	}

	/**
	 * Pega o nome do formato relacionado com o itag informado.
	 * 
	 * @param itag
	 *            tag do vídeo.
	 * @return nome do formato.
	 */
	private String getFormatString(int itag) {
		switch (itag) {
		case FLV_240P: {
			return "FLV 240p";
		}
		case FLV_270P: {
			return "FLV 270p";
		}
		case _3GP_144P: {
			return "3GP 144p";
		}
		case MP4_360P: {
			return "MP4 360p";
		}
		case MP4_720P: {
			return "MP4 720p";
		}
		case FLV_360P: {
			return "FLV 360p";
		}
		case FLV_480P: {
			return "FLV 480p";
		}
		case _3GP_240P: {
			return "3GP 240p";
		}
		case MP4_1080P: {
			return "MP4 1080p";
		}
		case MP4_3072P: {
			return "MP4 3072p";
		}
		case WEBM_360P: {
			return "WEBM 360p";
		}
		case WEBM_480P: {
			return "WEBM 480p";
		}
		case WEBM_720P: {
			return "WEBM 720p";
		}
		case WEBM_1080P: {
			return "WEBM 1080p";
		}
		}
		return null;
	}

	/**
	 * Pega o Itag de um determinado formato.
	 * 
	 * @param format
	 *            formato.
	 * @return itag.
	 */
	public int getItag(String format) {
		if (format.equals("FLV 240p"))
			return FLV_240P;
		else if (format.equals("FLV 270p"))
			return FLV_270P;
		else if (format.equals("3GP 144p"))
			return _3GP_144P;
		else if (format.equals("MP4 360p"))
			return MP4_360P;
		else if (format.equals("MP4 720p"))
			return MP4_720P;
		else if (format.equals("FLV 360p"))
			return FLV_360P;
		else if (format.equals("FLV 480p"))
			return FLV_480P;
		else if (format.equals("3GP 240p"))
			return _3GP_240P;
		else if (format.equals("MP4 1080p"))
			return MP4_1080P;
		else if (format.equals("MP4 3072p"))
			return MP4_3072P;
		else if (format.equals("WEBM 360p"))
			return WEBM_360P;
		else if (format.equals("WEBM 480p"))
			return WEBM_480P;
		else if (format.equals("WEBM 720p"))
			return WEBM_720P;
		else if (format.equals("WEBM 1080p"))
			return WEBM_1080P;
		return -1;
	}

	public VideoAttributes getVideoAttributes() {
		return this.videoAttributes;
	}

	public HashMap<String, String> getVideoUrls() {
		return this.videoUrls;
	}

	/**
	 * Cancela o download atual.
	 */
	public void cancelDownload() {
		downloadCancelado = true;
	}

	/**
	 * Interrompe a busca das Urls dos vídeos disponíveis para download.
	 */
	public void interruptUrlsGathering() {
		interrompido = true;
	}
}
