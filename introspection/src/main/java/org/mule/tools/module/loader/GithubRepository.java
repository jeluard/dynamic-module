package org.mule.tools.module.loader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

public class GithubRepository implements Repository {

    private static String ICON_RELATIVE_PATH = "/raw/master/";

    private final String url;
    private final org.eclipse.egit.github.core.Repository repository;
    private static final String URL_LAYOUT = ".*://github.com/(.*)/(.*)";

    public GithubRepository(final String url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("null url");
        }

        this.url = url;
        final Pattern pattern = Pattern.compile(GithubRepository.URL_LAYOUT);
        final Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid URL <"+url+">");
        }
        final String owner = matcher.group(1);
        final String name = matcher.group(2);
        this.repository = new RepositoryService(createClient()).getRepository(owner, name);
    }

    protected GitHubClient createClient() {
        return createDefaultClient();
    }

    protected final GitHubClient createDefaultClient() {
        return new GitHubClient();
    }

    @Override
    public URL getHomePage() {
        return asURL(this.repository.getHomepage());
    }

    @Override
    public URL getIconLocation(final String path) {
        final URL iconLocation = asURL(stripTrailing(this.url, '/')+GithubRepository.ICON_RELATIVE_PATH+stripLeading(normalize(path), '/'));
        try {
            iconLocation.openConnection().connect();
        } catch (IOException e) {
            return null;
        }
        return iconLocation;
    }

    private URL asURL(final String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            //Can't happen
            throw new RuntimeException(e);
        }
    }

    private String normalize(final String path) {
        if (path.startsWith("../")) {
            return normalize(path.substring(3));
        }
        return path;
    }

    private String stripLeading(final String text, final char character) {
        if (text.charAt(0) == character) {
            return text.substring(1);
        }
        return text;
    }

    private String stripTrailing(final String text, final char character) {
        if (text.charAt(text.length()-1) == character) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

}