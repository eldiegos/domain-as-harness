package customer

import (
	"errors"
	"regexp"
	"strings"
)

var emailRe = regexp.MustCompile(`^[^@\s]+@[^@\s]+\.[^@\s]+$`)

var ErrEmailTaken = errors.New("email already registered")

// Customer is a billing customer. Plan is "free" or "pro".
type Customer struct {
	ID    int64  `json:"id"`
	Name  string `json:"name"`
	Email string `json:"email"`
	Plan  string `json:"plan"`
}

// Validate checks the customer before it is stored.
func (c *Customer) Validate() error {
	if strings.TrimSpace(c.Name) == "" {
		return errors.New("name is required")
	}
	if !emailRe.MatchString(c.Email) {
		return errors.New("invalid email")
	}
	if c.Plan != "free" && c.Plan != "pro" {
		return errors.New("invalid plan")
	}
	return nil
}
