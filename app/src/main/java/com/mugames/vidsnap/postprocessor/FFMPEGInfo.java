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

package com.mugames.vidsnap.postprocessor;

public class FFMPEGInfo {


    public String videoMime;
    public String audioMime;
    public String localOutputMime;

    public String videoPath;
    public String audioPath;

    public String localOutputPath; //with in application's location Android/data/......
//    public String globalOutputPath;// Where user need their file to be....

    public String hlsURL;

    public FFMPEGInfo() {
    }



}
