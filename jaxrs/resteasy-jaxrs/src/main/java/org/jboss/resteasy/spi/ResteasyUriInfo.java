package org.jboss.resteasy.spi;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.specimpl.PathSegmentImpl;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.jboss.resteasy.util.Encode;
import org.jboss.resteasy.util.PathHelper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * UriInfo implementation with some added extra methods to help process requests
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyUriInfo implements UriInfo
{
   private String path;
   private String encodedPath;
   private String matchingPath;
   private MultivaluedMap<String, String> queryParameters;
   private MultivaluedMap<String, String> encodedQueryParameters;
   private MultivaluedMap<String, String> pathParameters;
   private MultivaluedMap<String, String> encodedPathParameters;
   private MultivaluedMap<String, PathSegment[]> pathParameterPathSegments;
   private MultivaluedMap<String, PathSegment[]> encodedPathParameterPathSegments;

   private List<PathSegment> pathSegments;
   private List<PathSegment> encodedPathSegments;
   private URI absolutePath;
   private URI requestURI;
   private URI baseURI;
   private List<String> matchedUris;
   private List<String> encodedMatchedUris;
   private List<String> encodedMatchedPaths = new LinkedList<String>();
   private List<Object> ancestors;

   public ResteasyUriInfo(URI base, URI relative)
   {
     setRequestUri(base, relative);
   }

   protected void processPath()
   {
      PathSegmentImpl.SegmentParse parse = PathSegmentImpl.parseSegmentsOptimization(encodedPath, false);
      encodedPathSegments = parse.segments;
      this.pathSegments = new ArrayList<PathSegment>(encodedPathSegments.size());
      for (PathSegment segment : encodedPathSegments)
      {
         pathSegments.add(new PathSegmentImpl(((PathSegmentImpl) segment).getOriginal(), true));
      }
      extractParameters(requestURI.getRawQuery());
      if (parse.hasMatrixParams) extractMatchingPath(encodedPathSegments);
      else matchingPath = encodedPath;

   }

   /**
    * matching path without matrix parameters
    *
    * @param encodedPathSegments
    */
   protected void extractMatchingPath(List<PathSegment> encodedPathSegments)
   {
      StringBuilder preprocessedPath = new StringBuilder();
      for (PathSegment pathSegment : encodedPathSegments)
      {
         preprocessedPath.append("/").append(pathSegment.getPath());
      }
      matchingPath = preprocessedPath.toString();
   }

   /**
    * Encoded path without matrix parameters
    *
    * @return
    */
   public String getMatchingPath()
   {
      return matchingPath;
   }

   /**
    * Updates the UriInfo with a new requestURI, keeping the same baseURI.
    * <p>
    * This can only be called before resource matching.
    */
   public void setRequestUri(URI requestURI) throws IllegalStateException
   {
      setRequestUri(this.baseURI, requestURI);
   }

   /**
    * Updates the UriInfo with a new baseURI and requestURI.
    * <p>
    * This can only be called before resource matching.
    */
   public void setRequestUri(URI baseURI, URI requestURI) throws IllegalStateException
   {
      if (matchedUris != null || !matchedUris.isEmpty()
          || encodedMatchedPaths != null || !encodedMatchedPaths.isEmpty()
          || encodedMatchedUris != null || !encodedMatchedUris.isEmpty()
          || ancestors != null || !ancestors.isEmpty()) {
         throw new IllegalStateException("setRequestUri can only be called before resource matching");
      }

      requestURI = baseURI.relativize(requestURI);

      String b = baseURI.toString();
      if (!b.endsWith("/")) b += "/";
      String r = requestURI.getRawPath();
      if (r.startsWith("/"))
      {
        encodedPath =  r;
        path = requestURI.getPath();
      }
      else
      {
        encodedPath = "/" + r;
        path = "/" + requestURI.getPath();
      }
      this.requestURI = requestURI;
      absolutePath = UriBuilder.fromUri(requestURI).replaceQuery(null).build();
      this.baseURI = baseURI;
      processPath();
   }

   public String getPath()
   {
      return path;
   }

   public String getPath(boolean decode)
   {
      if (decode) return getPath();
      return encodedPath;
   }

   public List<PathSegment> getPathSegments()
   {
      return pathSegments;
   }

   public List<PathSegment> getPathSegments(boolean decode)
   {
      if (decode) return getPathSegments();
      return encodedPathSegments;
   }

   public URI getRequestUri()
   {
      return requestURI;
   }

   public UriBuilder getRequestUriBuilder()
   {
      return UriBuilder.fromUri(requestURI);
   }

   public URI getAbsolutePath()
   {
      return absolutePath;
   }

   public UriBuilder getAbsolutePathBuilder()
   {
      return UriBuilder.fromUri(absolutePath);
   }

   public URI getBaseUri()
   {
      return baseURI;
   }

   public UriBuilder getBaseUriBuilder()
   {
      return UriBuilder.fromUri(baseURI);
   }

   public MultivaluedMap<String, String> getPathParameters()
   {
      if (pathParameters == null)
      {
         pathParameters = new MultivaluedMapImpl<String, String>();
      }
      return pathParameters;
   }

   public void addEncodedPathParameter(String name, String value)
   {
      getEncodedPathParameters().add(name, value);
      String value1 = Encode.decodePath(value);
      getPathParameters().add(name, value1);
   }

   private MultivaluedMap<String, String> getEncodedPathParameters()
   {
      if (encodedPathParameters == null)
      {
         encodedPathParameters = new MultivaluedMapImpl<String, String>();
      }
      return encodedPathParameters;
   }

   public MultivaluedMap<String, PathSegment[]> getEncodedPathParameterPathSegments()
   {
      if (encodedPathParameterPathSegments == null)
      {
         encodedPathParameterPathSegments = new MultivaluedMapImpl<String, PathSegment[]>();
      }
      return encodedPathParameterPathSegments;
   }

   public MultivaluedMap<String, PathSegment[]> getPathParameterPathSegments()
   {
      if (pathParameterPathSegments == null)
      {
         pathParameterPathSegments = new MultivaluedMapImpl<String, PathSegment[]>();
      }
      return pathParameterPathSegments;
   }

   public MultivaluedMap<String, String> getPathParameters(boolean decode)
   {
      if (decode) return getPathParameters();
      return getEncodedPathParameters();
   }

   public MultivaluedMap<String, String> getQueryParameters()
   {
      if (queryParameters == null)
      {
         queryParameters = new MultivaluedMapImpl<String, String>();
      }
      return queryParameters;
   }

   protected MultivaluedMap<String, String> getEncodedQueryParameters()
   {
      if (encodedQueryParameters == null)
      {
         this.encodedQueryParameters = new MultivaluedMapImpl<String, String>();
      }
      return encodedQueryParameters;
   }


   public MultivaluedMap<String, String> getQueryParameters(boolean decode)
   {
      if (decode) return getQueryParameters();
      else return getEncodedQueryParameters();
   }

   protected void extractParameters(String queryString)
   {
      if (queryString == null || queryString.equals("")) return;

      String[] params = queryString.split("&");

      for (String param : params)
      {
         if (param.indexOf('=') >= 0)
         {
            String[] nv = param.split("=", 2);
            try
            {
               String name = URLDecoder.decode(nv[0], "UTF-8");
               String val = nv.length > 1 ? nv[1] : "";
               getEncodedQueryParameters().add(name, val);
               getQueryParameters().add(name, URLDecoder.decode(val, "UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
         else
         {
            try
            {
               String name = URLDecoder.decode(param, "UTF-8");
               getEncodedQueryParameters().add(name, "");
               getQueryParameters().add(name, "");
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
      }
   }

   public List<String> getMatchedURIs(boolean decode)
   {
      if (decode)
      {
         if (matchedUris == null) matchedUris = new LinkedList<String>();
         return matchedUris;
      }
      else
      {
         if (encodedMatchedUris == null) encodedMatchedUris = new LinkedList<String>();
         return encodedMatchedUris;
      }
   }

   public List<String> getMatchedURIs()
   {
      return getMatchedURIs(true);
   }

   public List<Object> getMatchedResources()
   {
      if (ancestors == null) ancestors = new LinkedList<Object>();
      return ancestors;
   }


   public void pushCurrentResource(Object resource)
   {
      if (ancestors == null) ancestors = new LinkedList<Object>();
      ancestors.add(0, resource);
   }

   public void pushMatchedPath(String encoded)
   {
      encodedMatchedPaths.add(0, encoded);
   }

   public List<String> getEncodedMatchedPaths()
   {
      return encodedMatchedPaths;
   }

   public void popMatchedPath()
   {
      encodedMatchedPaths.remove(0);
   }



   public void pushMatchedURI(String encoded)
   {
      if (encoded.endsWith("/")) encoded = encoded.substring(0, encoded.length() - 1);
      if (encoded.startsWith("/")) encoded = encoded.substring(1);
      String decoded = Encode.decode(encoded);
      if (encodedMatchedUris == null) encodedMatchedUris = new LinkedList<String>();
      encodedMatchedUris.add(0, encoded);

      if (matchedUris == null) matchedUris = new LinkedList<String>();
      matchedUris.add(0, decoded);
   }

   @Override
   public URI resolve(URI uri)
   {
      return getBaseUri().resolve(uri);
   }

   @Override
   public URI relativize(URI uri)
   {
      URI from = getRequestUri();
      URI to = uri;
      if (uri.getScheme() == null && uri.getHost() == null)
      {
         to = getBaseUriBuilder().replaceQuery(null).path(uri.getPath()).replaceQuery(uri.getQuery()).fragment(uri.getFragment()).build();
      }
      return ResteasyUriBuilder.relativize(from, to);
   }

}