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
	"github.com/tj/go-dropbox"
	"github.com/tj/go-dropy"
	"fmt"
	"os"
	"io"
)

//pseudo const
var dropboxClientId = map[string]string{
	"trezor": "YOUR DROPBOX DATA",
	"keepkey": "YOUR DROPBOX DATA",
}
var dropboxClientSecret = map[string]string{
	"trezor": "YOUR DROPBOX DATA",
	"keepkey": "YOUR DROPBOX DATA",
}
var dropboxPath = map[string]string{
	"trezor": "YOUR DROPBOX DATA",
	"keepkey": "YOUR DROPBOX DATA",
}

var dropboxToken string
var db *dropy.Client
var downloadingFromDB bool

func DropboxToken(token string) {
	if token == "" {
		jc.SetDropboxStatus(false)
	} else if dropboxToken != token {
		dropboxToken = token
		go dropboxInit()
	}
}

func dropboxDownload(src, dst string) (err error) {
	src = "/" + src
	dst = "/" + dst

	data, err := db.Download(src)
	if err != nil {
		jc.SendToast("Error syncing with DropBox")
		return err
	}

	var fd *os.File
	if fd, err = os.Create(dst); err != nil {
		jc.SendToast("Error syncing with DropBox")
		return
	}
	defer fd.Close()
	defer data.Close()
	if _, err = io.Copy(fd, data); err != nil {
		os.Remove(dst)
	}

	return
}

func dropboxUpload(src, dst string) (err error) {
	src = "/" + src
	dst = "/" + dst


	var data *os.File
	data, err = os.Open(src)
	if err != nil {
		jc.SendToast("Error syncing with DropBox")
		return
	}

	err = db.Upload(dst, data)
	if err != nil {
		jc.SendToast("Error syncing with DropBox")
	}
	return err
}

func dropboxInit() {

	db = dropy.New(dropbox.New(dropbox.NewConfig(dropboxToken)))

	if err := db.Mkdir("/" + dropboxPath[deviceType][:len(dropboxPath[deviceType])-1]); err != nil {
		// Error creating folder (it already exists
		fmt.Println("DB INIT", err)
		downloadingFromDB = true
	} else {
		// Folder does not exists, nothing to download
	}
	jc.SetDropboxStatus(true)
}
