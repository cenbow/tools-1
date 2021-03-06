package tools.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.http.*;

import tools.Convert;
import tools.StringUtil;
import tools.Validate;

public class ServletUtil {
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String ETAG = "ETag";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String LAST_MODIFIED = "Last-Modified";

	// ============如果在filter的init方法里调用了registerFilter方法 2015-4-24 15:59:10 by liusan.dyf
	private static Map<String, Filter> filters = tools.MapUtil.create();

	public static Filter getFilter(String name) {
		if (name == null)
			return null;

		return filters.get(name);
	}

	public static void registerFilter(String name, Filter v) {
		if (name != null && v != null)
			filters.put(name, v);
	}

	// ============ end filter

	public static int getInt(HttpServletRequest request, String key, int def) {
		return Convert.toInt(request.getParameter(key), def);
	}

	public static long getLong(HttpServletRequest request, String key, long def) {
		return Convert.toLong(request.getParameter(key), def);
	}

	public static float getLong(HttpServletRequest request, String key, float def) {
		return Convert.toFloat(request.getParameter(key), def);
	}

	public static Date getDateTime(HttpServletRequest request, String key) {
		return Convert.toDateTime(request.getParameter(key));
	}

	public static String getString(HttpServletRequest request, String key) {
		return request.getParameter(key);
	}

	public static String generateETagHeaderValue(String s) {
		StringBuilder sb = new StringBuilder("\"0");
		sb.append(s);
		sb.append('"');
		return sb.toString();
	}

	/**
	 * 输出文本类的内容给HttpServletResponse，目前是js/css/html/htm 2015-9-10 14:37:34 by liusan.dyf
	 * 
	 * @param request
	 * @param response
	 * @param content
	 * @param charset
	 * @param lastUpdate
	 * @return
	 * @throws IOException
	 */
	public static boolean output(HttpServletRequest request, HttpServletResponse response, String content,
			String charset, Date lastUpdate) throws IOException {
		// ---判断参数
		if (request == null)
			return false;

		String v = content;
		if (tools.Validate.isNullOrEmpty(v))
			return false;

		if (lastUpdate == null)
			lastUpdate = new Date();

		// url作为etag的一部分
		String url = request.getRequestURI();

		// 得到etag，这里是对应value的hashcode 2015-6-5 21:24:13 by liusan.dyf
		// from http://www.infoq.com/cn/articles/etags

		String hashContent = url + "@" + tools.DateTime.format(lastUpdate, null);// 2015-9-10
		String etag = generateETagHeaderValue(String.valueOf(hashContent.hashCode()));

		response.setHeader(ETAG, etag); // always store the ETag in the header
		String requestETag = request.getHeader(IF_NONE_MATCH);

		// 比对etag 2015-6-5 21:32:08 by liusan.dyf
		if (etag.equals(requestETag)) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			response.setHeader(LAST_MODIFIED, request.getHeader("If-Modified-Since"));
			return true;
		}

		// 你需注意到，我们还设置了Last-Modified头。这被认为是为服务器产生内容的正确形式，因为其迎合了不认识ETag头的客户端。

		// 输出内容
		if (url.endsWith(".js")) // 一些js我们也放到缓存里去，方便部署和发布 2013-02-28
			response.addHeader(CONTENT_TYPE, "application/x-javascript; charset=" + charset);
		else if (url.endsWith(".css")) // 2013-04-19 by liusan.dyf
			response.addHeader(CONTENT_TYPE, "text/css; charset=" + charset);
		else if (url.endsWith(".htm") || url.endsWith(".html") || url.endsWith("/"))
			response.addHeader(CONTENT_TYPE, "text/html; charset=" + charset);

		response.getWriter().write(v);
		response.setDateHeader(LAST_MODIFIED, lastUpdate.getTime());

		return true;
	}

	/**
	 * 得到当前请求的域名信息，不以/结尾，比如【http://m.omeweb.com】 2015-9-1 14:19:47 by liusan.dyf
	 * 
	 * @param request
	 * @return
	 */
	public static String getServerUrl(HttpServletRequest request) {
		int port = request.getServerPort();

		StringBuilder result = new StringBuilder();
		result.append(request.getScheme()).append("://").append(request.getServerName());

		if (port != 80) {
			result.append(':').append(port);
		}

		return result.toString();
	}

	/**
	 * 得到请求的url，附带queryString参数，但是不包含post的参数 2015-6-19 10:38:13 by liusan.dyf
	 * 
	 * @param request
	 * @return
	 */
	public static String getRequestUrlWithQueryString(HttpServletRequest request) {
		String url = request.getRequestURL().toString();
		String queryString = request.getQueryString();

		if (Validate.isBlank(queryString))
			return url;
		else
			return url + "?" + queryString;
	}

	/**
	 * 是否是非法的IP
	 * 
	 * @param ip
	 * @return
	 */
	private static boolean invalidIp(String ip) {
		return ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip);
	}

	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");

		if (invalidIp(ip))
			ip = request.getHeader("Proxy-Client-IP");

		if (invalidIp(ip))
			ip = request.getHeader("WL-Proxy-Client-IP");

		if (invalidIp(ip)) // 2012-10-16 by liusan.dyf
			ip = request.getHeader("X-Real-IP");

		if (invalidIp(ip))
			ip = request.getRemoteAddr();

		return ip;

		/*
		 * from http://blog.sina.com.cn/s/blog_5198c7370100m5cu.html
		 * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串Ｉｐ值，究竟哪个才是真正的用户端的真实IP呢？ 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
		 * 如：X-Forwarded-For：192.168.1.110， 192.168.1.120， 192.168.1.130， 192.168.1.100用户真实IP为： 192.168.1.110
		 */
	}

	/**
	 * 以非key-value的方式往服务器里post数据，从该方法可以获取到这些数据 2015-8-19 12:11:40 by liusan.dyf
	 * 
	 * @param request
	 * @return
	 */
	public static String getHttpRawPostData(HttpServletRequest request) {
		StringBuffer sb = new StringBuffer();
		String line = null;

		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
				sb.append(line);
		} catch (Exception e) {
			/* report an error */
		}

		return sb.toString();
	}

	/**
	 * addHeader("Content-Type", "application/json") 2012-04-09
	 * 
	 * @param response
	 * @param charset
	 */
	public static void addJsonContentType(HttpServletResponse response, String charset) {
		addContentType(response, "application/json", charset);
	}

	/**
	 * 2013-12-19 by liusan.dyf
	 * 
	 * @param response
	 * @param contentType
	 * @param charset
	 */
	public static void addContentType(HttpServletResponse response, String contentType, String charset) {
		if (StringUtil.isNullOrEmpty(charset))
			// 编码
			response.addHeader(CONTENT_TYPE, contentType);
		else
			response.addHeader(CONTENT_TYPE, contentType + ";charset=" + charset);
	}

	/**
	 * request.getParameterMap()的返回类型是Map类型的对象，也就是符合key-value的对应关系，但这里要注 意的是，value的类型是String[],而不是String <br />
	 * 不会返回null，最多是空的map 2012-11-12 by liusan.dyf
	 * 
	 * @param request
	 * @param charset 可以为null
	 * @return
	 */
	public static Map<String, Object> getParameterMap(HttpServletRequest request, String charset) {
		Map<String, Object> rtn = tools.MapUtil.create();

		@SuppressWarnings("unchecked")
		Map<String, String[]> map = (Map<String, String[]>) request.getParameterMap();

		// 如果为空
		if (map == null || map.size() == 0)
			return rtn;

		Set<String> keys = map.keySet();
		for (String item : keys) {
			String[] value = map.get(item);
			String realValue = null;

			if (value.length == 1) // 只有一个值
				realValue = value[0];
			else
				realValue = Convert.join(value, ",");// 多值

			// 进行解码 2015-5-5 12:43:33 by liusan.dyf
			if (realValue != null) {
				try {
					if (charset != null)
						realValue = URLDecoder.decode(realValue, charset);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				rtn.put(item, realValue);
			}
		}

		return rtn;

		// request.getParameterMap()的返回类型是Map类型的对象，也就是符合key-value的对应关系，但这里要注意的是，value的类型是String[],而不是String.
		// 得到jsp页面提交的参数很容易,但通过它可以将request中的参数和值变成一个map，以下是将得到的参数和值
		// 打印出来，形成的map结构：map(key,value[])，即：key是String型，value是String型数组。
		// 例如：request中的参数t1=1&t1=2&t2=3
		// 形成的map结构：
		// key=t1;value[0]=1,value[1]=2
		// key=t2;value[0]=3
		// 如果直接用map.get("t1"),得到的将是:Ljava.lang.String; value只所以是数组形式，就是防止参数名有相同的情况。
	}
}
