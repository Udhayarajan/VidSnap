/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mugames.vidsnap.ui.main.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;



/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {

    String TAG= Statics.TAG+":LoginFragment";

    private static final String ARG_LINK = "urlORLink";
    private static final String ARG_ON_LOGGED_IN = "logged_in";

    private String mUrl;
    boolean signedIn;
    String[] loginDoneUrls;
    WebView webView;
    UtilityInterface.CookiesInterface cookiesInterface;
    String cookies;

    public LoginFragment() {
        // Required empty public constructor
    }


    public static LoginFragment newInstance(String url, String[] loginDoneUrls, String cookies, UtilityInterface.CookiesInterface cookiesInterface) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LINK, url);
        args.putStringArray(ARG_ON_LOGGED_IN, loginDoneUrls);
        fragment.setArguments(args);
        fragment.cookies=cookies;
        fragment.cookiesInterface = cookiesInterface;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUrl = getArguments().getString(ARG_LINK);
            loginDoneUrls = getArguments().getStringArray(ARG_ON_LOGGED_IN);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        webView = view.findViewById(R.id.webView);
        webView.setWebViewClient(new CustomWebClient());
        WebSettings webSettings=webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl(mUrl);
        return view;
    }


    class CustomWebClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG, "onPageFinished: " + url);
            if (isFinalUrl(url) && !signedIn) {
                Log.d(TAG, "onPageFinished: InstagramSigned in");
                String cookies = CookieManager.getInstance().getCookie(url);
                Log.d(TAG, "All the cookies in a string:" + cookies);
                while (webView.canGoBack())
                    webView.goBack();
                cookiesInterface.onReceivedCookies(cookies);
                signedIn=true;
            }
        }

        boolean isFinalUrl(String url){
            for (String tURL: loginDoneUrls) {
                if(tURL.equals(url)) return true;
            }
            return false;
        }
    }
}