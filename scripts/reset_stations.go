package main

import (
	"fmt"
	"github.com/cosn/firebase"
	"log"
	"time"
)

type Station struct {
	Code        string `json:"CODE_LIGNE"`
	Name        string `json:"NOM"`
	Latitude    string `json:"LATITUDE"`
	Longitude   string `json:"LONGITUDE"`
	Owner       string `json:"owner"`
	OwnerMail   string `json:"OwnerMail"`
	When        int64  `json:"when"`
}

func main() {
	client := new(firebase.Client)
	client.Init("https://shining-inferno-9452.firebaseio.com", "a6O8UkBnRaeQ88BjUHv46VMNFBAhUDwKdFDfcfYz", nil)

	for i := 1; i < 6441; i++ {
		station := &Station{};
		stationChild := client.Child("stations", nil, nil).Child(fmt.Sprintf("%v", i), nil, station)
		if (station.Owner != "") {
			if (time.Now().UnixNano()/int64(time.Millisecond)-station.When > 5*3600*1000) {
				log.Printf("Free station : %v", i)
				stationChild.Set("", &Station{Code: station.Code, Name: station.Name, Latitude: station.Latitude, Longitude: station.Longitude }, nil)
			}
		}
	}
}
