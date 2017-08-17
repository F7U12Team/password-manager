/*This is distributed under the Apache License v2.0

Copyright 2017 F7U12 Team - pma@madriguera.me

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
	"errors"

	"bufio"
	"encoding/hex"
	"encoding/json"
	"os"

	"io/ioutil"
	"strconv"

	"fmt"

	"github.com/conejoninja/cerrojo"
	"github.com/conejoninja/cerrojo/devices"
	"github.com/conejoninja/cerrojo/transport"
	"github.com/conejoninja/hid"
	"github.com/stacktic/dropbox"
	"encoding/csv"
	"strings"
	"io"
	"hash/fnv"
)

var client cerrojo.Client
var device hid.Device
var jc JavaCallback
var status int

var masterKey string
var filename string
var encKey string
var dataStorage cerrojo.Storage

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

const appPath string = "/data/data/your.app.path.here/files"
const allowSyncMerge = true
const autoPassphrase = true

var dropboxToken string
var db *dropbox.Dropbox
var downloadingFromDB bool
var deviceType string

const (
	emptyValue = iota
	getlogins
	routinegetlogins
	getlogin
	savelogin
	routinegetlogin
)

const (
	syncNone = iota
	syncOffline
	syncDropbox
)

type EncData struct {
	Id       string `json:"id,omitempty"`
	Password string `json:"password,omitempty"`
	SafeNote string `json:"safenote,omitempty"`
}

type Entry struct {
	entry cerrojo.Entry
	id string
}

const emptyFileContent = `{"version":"0.0.1","config":{"orderType":"note"},"tags":{"0":{"title":"All","icon":"home"},"1":{"title":"Social","icon":"person-stalker"},"2":{"title":"Bitcoin","icon":"social-bitcoin"}},"entries":{}}`

type JavaCallback interface {
	SendEvent(string, string)
	SendToast(string)
	SetDropboxStatus(bool)
}

func RegisterJavaCallback(c JavaCallback) {
	jc = c
}

func Connect(fdint int, path, dt string) {

	deviceType = dt
	devicee := hid.GetUsbDevice()
	device = &devicee

	var fd uintptr
	fd = uintptr(fdint)

	device.SetFD(fd)

	// This should be from Java
	var info hid.Info
	info.Vendor = 21324
	info.Product = 1
	info.Revision = 256
	info.SubClass = 0
	info.Protocol = 0
	info.Interface = 0
	device.SetInfo(info)
	device.SetEpIn(129)
	device.SetEpOut(1)
	device.SetPacketSize(64)
	device.SetPath(path) //"/dev/bus/usb/001/001")

	var t transport.TransportHIDAndroid
	t.SetDevice(device)
	d := devices.GetDevice(dt)
	client.SetTransport(&t, d)

	go GetLogins()
	return
}

func DropboxToken(token string) {
	if token == "" {
		jc.SetDropboxStatus(false)
	} else if dropboxToken != token {
		dropboxToken = token
		go dropboxInit()
	}
}

func dropboxDownload(src, dst string) error {
	return db.DownloadToFile(src, dst, "")
}

func dropboxUpload(src, dst string) (err error) {
	_, err = db.UploadFile(src, dst, true, "")
	if err != nil {
		jc.SendEvent("goErrorDropbox", "")
	}
	return err
}

func dropboxInit() {
	db = dropbox.NewDropbox()
	db.SetAppInfo(dropboxClientId[deviceType], dropboxClientSecret[deviceType])
	db.SetAccessToken(dropboxToken)
	if _, err := db.CreateFolder(dropboxPath[deviceType]); err != nil {
		fmt.Printf("-=[DROPBOX]=- Error creating folder %s: %s\n", dropboxPath[deviceType], err)
		downloadingFromDB = true
	} else {
		fmt.Printf("-=[DROPBOX]=- Folder %s successfully created\n", dropboxPath[deviceType])
	}
	jc.SetDropboxStatus(true)
}

func call(msg []byte) (string, uint16) {
	str, msgType := client.Call(msg)

	if msgType == 18 {
		jc.SendEvent("goPinDialog", "0")
	} else if msgType == 26 {
		jc.SendEvent("goInstructionsDialog", "")
		str, msgType = call(client.ButtonAck())
	} else if msgType == 41 {
		if autoPassphrase {
			str, msgType = call(client.PassphraseAck(""))
		} else {
			jc.SendEvent("goPassphraseDialog", "")
		}
	} else if msgType == 46 {
		jc.SendEvent("goWordDialog", "")
	}
	if msgType == 48 {
		if status == getlogins {
			status = emptyValue
			routineGetLogins(str)
		} else if status == routinegetlogins {
			status = emptyValue
		} else if status == routinegetlogin {
			status = emptyValue
		} else {
		}
	}

	return str, msgType
}

func InputPin(input string) {

	jc.SendEvent("goInstructionsDialog", "")
	go inputPinThread(input)
	return
}

func inputPinThread(input string) {

	_, msgType := call(client.PinMatrixAck(input))
	if msgType == 3 {
		go GetLogins()
		return
	}

	return
}

func InputPassphrase(input string) (result string, err error) {
	result, msgType := call(client.PassphraseAck(input))
	if msgType != 2 {
		err = errors.New("Wrong Passphrase")
		//jc.SendEvent("goPassphraseDialog", "")
	}
	return
}

func InputWord(input string) (result string, err error) {
	result, msgType := call(client.WordAck(input))
	if msgType != 2 {
		err = errors.New("Wrong Word")
		//jc.SendEvent("goWordDialog", "")
	}
	return
}

func GetLogins() {

	// GET MASTER KEY
	status = getlogins
	_, msgType := call(client.GetMasterKey())
	// TODO REMOVE
	if msgType == 18 {
		// ASKED FOR PIN
		// EXECUTION STOPPED
		// WAIT FOR PIN FROM JAVA
	} else if msgType == 48 {
		// EVERYTHING OK
		//result, err = routineGetLogins(str)
		return
	}
	return
}

func routineGetLogins(str string) {
	status = routinegetlogins
	masterKey = hex.EncodeToString([]byte(str))
	filename, _, encKey = cerrojo.GetFileEncKey(masterKey)

	var content string
	var contentByte []byte
	var errOffline error
	var errDropbox error
	var data cerrojo.Storage

	// OPEN FILE
	contentByte, errOffline = readFile(appPath + "/" + filename)
	if errOffline != nil {
		fmt.Println("Error opening file " + filename)
		jc.SendEvent("goSetDataList", emptyFileContent)

		errOffline = json.Unmarshal([]byte(emptyFileContent), &data)
		if errOffline == nil {
			efile := cerrojo.EncryptStorage(data, encKey)
			writeFile(filename, efile, false)
		}

	} else {
		content = string(contentByte)
		data, errOffline = cerrojo.DecryptStorage(content, encKey)
	}

	if downloadingFromDB {
		jc.SendEvent("goWaitSyncDialog", "")
		downloadingFromDB = false
		errDropbox = dropboxDownload(dropboxPath[deviceType]+filename, appPath+"/tmp.pswd")
		if errDropbox == nil {
			contentByte, errDropbox = readFile(appPath + "/tmp.pswd")
			if errDropbox == nil {
				contentDB := string(contentByte)
				if contentDB != content {
					// Use Dropbox version, we should do the merge here
					if allowSyncMerge {
						var dataDB cerrojo.Storage
						dataDB, errDropbox = cerrojo.DecryptStorage(contentDB, encKey)
						data = mergeStorages(dataDB, data)
						if errDropbox == nil {
							efile := cerrojo.EncryptStorage(data, encKey)
							writeFile(filename, efile, true)
						}
					} else {
						content = contentDB
						writeFile(filename, contentByte, false)
					}
					deleteFile("tmp.pswd")
				}
			}
		}
	}

	if errOffline != nil && errDropbox != nil {
		return
	}

	// DECRYPT STORAGE
	dataStorage = data
	resultByte, err := json.Marshal(data)
	if err != nil {
		fmt.Println(err)
	}
	// Send data to java
	jc.SendEvent("goSetDataList", string(resultByte))

	return
}

func GetLogin(id string, edit bool) (result string, err error) {

	if edit {
		jc.SendEvent("goInstructionsDialog", "")
	}
	go routineGetLogin(id, edit)

	return result, nil
}

func routineGetLogin(id string, edit bool) {
	if dataStorage.Entries != nil {
		if _, ok := dataStorage.Entries[id]; ok {
			status = routinegetlogin
			str, _ := call(client.GetEntryNonce(cerrojo.CleanURL(dataStorage.Entries[id].Title), dataStorage.Entries[id].Username, dataStorage.Entries[id].Nonce))
			pswd, e1 := cerrojo.DecryptEntry(string(dataStorage.Entries[id].Password.Data), str)
			note, e2 := cerrojo.DecryptEntry(string(dataStorage.Entries[id].SafeNote.Data), str)
			if e1 != nil || e2 != nil {
				str, _ = call(client.GetEntryNonce(dataStorage.Entries[id].Title, dataStorage.Entries[id].Username, dataStorage.Entries[id].Nonce))
				pswd, _ = cerrojo.DecryptEntry(string(dataStorage.Entries[id].Password.Data), str)
				note, _ = cerrojo.DecryptEntry(string(dataStorage.Entries[id].SafeNote.Data), str)
			}

			var ed EncData
			ed.Id = id
			if len(pswd) >= 2 {
				ed.Password = pswd[1 : len(pswd)-1]
			} else {
				ed.Password = ""
			}
			if len(note) >= 2 {
				ed.SafeNote = note[1 : len(note)-1]
			} else {
				ed.SafeNote = ""
			}

			j, e := json.Marshal(ed)
			if e != nil {
				jc.SendEvent("goErrorDialog", "Error with the encrypted data")
				return
			} else {
				if edit {
					jc.SendEvent("goEditData", string(j))
				} else {
					jc.SendEvent("goSetData", string(j))
				}
			}
		} else {
			jc.SendEvent("goErrorDialog", "Entry not found")
			return
		}
	} else {
		jc.SendEvent("goErrorDialog", "Entry not found")
		return
	}
}

func SaveLogin(title, username, note, password, secretNote, id string) {

	// GET MASTER KEY
	status = savelogin
	// EVERYTHING OK

	var entry cerrojo.Entry
	entry.Title = title
	entry.Username = username
	entry.Note = note
	nonceByte, _ := cerrojo.GenerateRandomBytes(32)
	nonce := string(nonceByte)
	entry.Tags = []int{1}
	var eNonce string
	eNonce, _ = call(client.SetEntryNonce(cerrojo.CleanURL(entry.Title), entry.Username, nonce))
	entry.Nonce = hex.EncodeToString([]byte(eNonce))
	entry.Password = cerrojo.EncryptedData{"Buffer", cerrojo.EncryptEntry("\""+password+"\"", nonce)}
	entry.SafeNote = cerrojo.EncryptedData{"Buffer", cerrojo.EncryptEntry("\""+secretNote+"\"", nonce)}

	var e Entry
	e.entry = entry
	e.id = id

	entries := make([]Entry, 1)
	entries[0] = e

	go routineSaveLogin(entries)
	return
}

func routineSaveLogin(entries []Entry) {

	for _, e := range entries {
		lastEntry := "0"
		if dataStorage.Entries == nil {
			dataStorage.Entries = make(map[string]cerrojo.Entry)
		}

		if e.id == "" {
			max := 0
			for k, _ := range dataStorage.Entries {
				i, e := strconv.Atoi(k)
				if e == nil && i > max {
					max = i
				}
			}
			lastEntry = strconv.Itoa(max + 1)
		} else {
			lastEntry = e.id
		}

		dataStorage.Entries[lastEntry] = e.entry
	}


	efile := cerrojo.EncryptStorage(dataStorage, encKey)
	writeFile(filename, efile, true)

	resultByte, errb := json.Marshal(dataStorage)
	if errb != nil {
		jc.SendEvent("goErrorDialog", "Error Marshalling dataStorage")
		//return string(resultByte), errb
	}
	// Send data to java
	jc.SendEvent("goSetDataList", string(resultByte))
	return
}

func DeleteLogin(id string) {
	jc.SendEvent("goWaitSynDialog", "")
	go routineDeleteLogin(id)
}

func routineDeleteLogin(k string) {
	if _, ok := dataStorage.Entries[k]; ok {
		delete(dataStorage.Entries, k)

		efile := cerrojo.EncryptStorage(dataStorage, encKey)
		writeFile(filename, efile, true)

		resultByte, errb := json.Marshal(dataStorage)
		if errb != nil {
			jc.SendEvent("goErrorDialog", "Error Marshalling dataStorage")
			//return string(resultByte), errb
		}
		// Send data to java
		jc.SendEvent("goSetDataList", string(resultByte))
	}
}

func CleanData() {
	masterKey = ""
	filename = ""
	encKey = ""
	dataStorage = cerrojo.Storage{}
}

func readFile(filename string) ([]byte, error) {
	var empty []byte

	file, err := os.Open(filename)
	defer file.Close()
	if err != nil {
		return empty, err
	}

	stats, statsErr := file.Stat()
	if statsErr != nil {
		return empty, statsErr
	}
	var size int64 = stats.Size()
	fw := make([]byte, size)

	bufr := bufio.NewReader(file)
	_, err = bufr.Read(fw)
	return fw, err
}

func writeFile(filename string, content []byte, upload bool) {
	errDir := os.MkdirAll(appPath, 0777)
	if errDir != nil {
		jc.SendEvent("goErrorDialog", "Unable to create directory")
		return
	}
	err := ioutil.WriteFile(appPath+"/"+filename, content, 0644)
	if err != nil {
		fmt.Println("Unable to create File", err)
		jc.SendEvent("goErrorDialog", "Unable to create file")
		return
	}
	if upload && dropboxToken != "" {
		// ASSUME TOKEN = USIG DROPBOX
		go dropboxUpload(appPath+"/"+filename, dropboxPath[deviceType]+filename)
	}
}

func deleteFile(filename string) {
	err := os.Remove(appPath + "/" + filename)
	if err != nil {
		// better to fail silently
		//jc.SendEvent("goErrorDialog", "Unable to create directory")
		return
	}
}

func mergeStorages(one, two cerrojo.Storage) cerrojo.Storage {

	if one.Entries == nil {
		one.Entries = make(map[string]cerrojo.Entry)
	}

	for _, v := range two.Entries {
		found := false
		for _, onev := range one.Entries {
			if v.Equal(onev) {
				fmt.Println("FOUND", v, onev)
				found = true
				break
			}
		}
		if !found {
			lastEntry := "0"
			max := 0
			for k, _ := range one.Entries {
				i, e := strconv.Atoi(k)
				if e == nil && i > max {
					max = i
				}
			}
			lastEntry = strconv.Itoa(max + 1)
			one.Entries[lastEntry] = v
			fmt.Println("LAST ENTRY", lastEntry)
		}
	}


	if one.Tags == nil {
		one.Tags = make(map[string]cerrojo.Tag)
	}

	for k, v := range two.Tags {
		found := false
		for onek, onev := range one.Tags {
			if k == onek {
				fmt.Println("FOUND", v, onev)
				found = true
				break
			}
		}
		if !found {
			one.Tags[k] = v
		}
	}
	return one
}
func ImportFile(file, format string) {
	go routineImportFile(file, format)
}
func routineImportFile(file, format string) {
	errStr := "Error importing file:"
	content, err := readFile(file)
	fmt.Println("IMPORTING FILE", file)
	if err != nil {
		jc.SendToast(fmt.Sprint(errStr,err))
	}
	if format == "lastpass" {
		r := csv.NewReader(strings.NewReader(string(content)))

		entriesMap := make(map[uint32]Entry)
		for {
			record, err := r.Read()
			if err == io.EOF {
				break
			}
			if err != nil {
				fmt.Println("ERROR FOR", err)
			}

			if len(record) == 7 {
				if record[0] != "url" ||
					record[1] != "username" ||
					record[2] != "password" ||
					record[3] != "extra" ||
					record[4] != "name" ||
					record[5] != "grouping" ||
					record[6] != "fav" {

					hash := simpleHash(strings.Join(record, "-"))

					var entry cerrojo.Entry
					entry.Title = record[0]
					entry.Username = record[1]
					entry.Note = record[4]
					nonceByte, _ := cerrojo.GenerateRandomBytes(32)
					nonce := string(nonceByte)
					entry.Tags = []int{1}
					var eNonce string
					eNonce, _ = call(client.SetEntryNonce(cerrojo.CleanURL(entry.Title), entry.Username, nonce))
					entry.Nonce = hex.EncodeToString([]byte(eNonce))
					entry.Password = cerrojo.EncryptedData{"Buffer", cerrojo.EncryptEntry("\""+record[2]+"\"", nonce)}
					entry.SafeNote = cerrojo.EncryptedData{"Buffer", cerrojo.EncryptEntry("\""+record[3]+"\"", nonce)}

					var e Entry
					e.entry = entry
					e.id = ""

					entriesMap[hash] = e

				}
			}
		}

		entries := make([]Entry, len(entriesMap))
		k := 0
		for _, e := range entriesMap {
			entries[k] = e
			k++
		}

		routineSaveLogin(entries)

		jc.SendToast("File imported")

	} else {
		jc.SendToast(fmt.Sprint(errStr,"format unknown"))
	}
}


func simpleHash(s string) uint32 {
	h := fnv.New32a()
	h.Write([]byte(s))
	return h.Sum32()
}