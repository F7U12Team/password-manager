/*This is distributed under the Apache License v2.0

Copyright 2017-2018 F7U12 Team - pma@madriguera.me

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package cerrojoandroid

import (
	"fmt"
	"io"
	"os"
	"strings"

	"github.com/pkg/errors"
	"golang.org/x/net/context"
	"golang.org/x/oauth2/google"
	"google.golang.org/api/drive/v3"
)

var googleDriveToken string
var downloadingFromGD bool
var gdriveService *drive.Service

func GoogleDriveToken(token string) {
	if token == "" {
		jc.SetGoogleDriveStatus(false)
	} else if googleDriveToken != token {
		googleDriveToken = token
		go googleDriveInit()
	}
}

func googleDriveInit() {

	jc.SendToast(fmt.Sprint("Syncing with Google Drive"))

	ctx := context.Background()

	b := []byte("JSON CONTENTS OF GOOGLE DRIVE OAUTH CREDENTIALS")

	config, err := google.ConfigFromJSON(b, drive.DriveScope)
	if err != nil {
		fmt.Printf("Unable to parse client secret file to config: %v", err)
		jc.SendToast(fmt.Sprint("Failed syncing with Google Drive"))
		return
	}
	tok, err := config.Exchange(ctx, googleDriveToken)
	if err != nil {
		fmt.Printf("Unable to retrieve token from web %v", err)
		jc.SendToast(fmt.Sprint("Failed syncing with Google Drive"))
		return
	}

	client := config.Client(ctx, tok)
	gdriveService, err = drive.New(client)

	if err != nil {
		fmt.Printf("Unable to retrieve drive Client %v", err)
		jc.SendToast(fmt.Sprint("Failed syncing with Google Drive"))
		return
	}

	var folderID string
	folderID, err = googleDriveCreateFolderSafe(dropboxPath[deviceType])
	if err == nil && folderID != "" {
		/*var r *drive.FileList
		r, err = gdriveService.Files.
			List().
			PageSize(1).
			Fields("nextPageToken, files(id, name)").
			Q("name = '" + filename + "' and '" + folderID + "' in parents").
			Do()
			fmt.Println("SHOULD DOWNLOAD?", len(r.Files), "name = '" + filename + "' and '" + folderID + "' in parents")
		if err == nil && len(r.Files) > 0 {
			downloadingFromGD = true
		} */
		downloadingFromGD = true
	}

	jc.SetGoogleDriveStatus(true)
}

func googleDriveUpload(src, dst string) (err error) {

	paths := strings.Split(dst, "/")
	l := len(paths)
	dstPath := strings.Join(paths[:l-1], "/")
	filename := paths[l-1]
	folderID, err := googleDriveCreateFolderSafe(dstPath)

	var fd drive.File
	fd.Name = filename

	var data *os.File
	data, err = os.Open(src)
	if err != nil {
		return
	}

	var r *drive.FileList
	r, err = gdriveService.Files.
		List().
		PageSize(1).
		Fields("nextPageToken, files(id, name)").
		Q("name = '" + filename + "' and '" + folderID + "' in parents").
		Do()
	if err != nil {
		return
	}

	if len(r.Files) == 0 {
		fd.Parents = []string{folderID}
		gdriveService.Files.Create(&fd).Media(data).Do()
	} else {
		gdriveService.Files.Update(r.Files[0].Id, &fd).Media(data).Do()
	}

	return

}

func googleDriveDownload(src, dst string) error {

	paths := strings.Split(src, "/")
	l := len(paths)
	srcPath := strings.Join(paths[:l-1], "/")
	filename := paths[l-1]
	folderID, err := googleDriveCreateFolderSafe(srcPath)

	if err != nil {
		return err
	}
	if folderID == "" {
		return errors.New("Empty folder ID")
	}

	var r *drive.FileList
	r, err = gdriveService.Files.
		List().
		PageSize(1).
		Fields("nextPageToken, files(id, name)").
		Q("name = '" + filename + "' and '" + folderID + "' in parents").
		Do()

	if err != nil {
		return err
	}
	if len(r.Files) == 0 {
		return errors.New("No file on GoogleDrive to download")
	}

	dstFile := gdriveService.Files.Get(r.Files[0].Id)
	data, err := dstFile.Download()
	if err != nil {
		return err
	}
	var fd *os.File

	if fd, err = os.Create(dst); err != nil {
		return err
	}
	defer fd.Close()
	defer data.Body.Close()
	if _, err = io.Copy(fd, data.Body); err != nil {
		os.Remove(dst)
	}
	return err
}

func googleDriveCreateFolderSafe(path string) (parentFolder string, err error) {

	var tmpF *drive.File
	var r *drive.FileList
	folders := strings.Split(path, "/")
	for depth, folder := range folders {
		if folder == "" {
			continue
		}
		r, err = gdriveService.Files.
			List().
			PageSize(1).
			Fields("nextPageToken, files(id, name)").
			Q("name = '" + folder + "'").
			Do()
		if err != nil {
			return
		}

		if len(r.Files) == 0 {
			var fd drive.File
			fd.Name = folder
			fd.MimeType = "application/vnd.google-apps.folder"

			if depth != 0 {
				fd.Parents = []string{parentFolder}
			}
			tmpF, err = gdriveService.Files.Create(&fd).Fields("id").Do()
			if err != nil {
				return
			}
			parentFolder = tmpF.Id
		} else {
			parentFolder = r.Files[0].Id
		}
	}
	return
}
