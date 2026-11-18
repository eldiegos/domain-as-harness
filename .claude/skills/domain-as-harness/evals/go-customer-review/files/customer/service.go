package customer

import (
	"context"
	"encoding/csv"
	"io"
	"strings"
)

type Service struct {
	repo *Repo
}

func NewService(repo *Repo) *Service {
	return &Service{repo: repo}
}

// Register validates and stores a new customer.
func (s *Service) Register(ctx context.Context, c *Customer) (int64, error) {
	if err := c.Validate(); err != nil {
		return 0, err
	}
	exists, err := s.repo.EmailExists(ctx, c.Email)
	if err != nil {
		return 0, err
	}
	if exists {
		return 0, ErrEmailTaken
	}
	return s.repo.Insert(ctx, c)
}

// ChangeEmail updates the email of an existing customer.
func (s *Service) ChangeEmail(ctx context.Context, id int64, email string) error {
	c, err := s.repo.FindByID(ctx, id)
	if err != nil {
		return err
	}
	c.Email = strings.TrimSpace(email)
	if err := c.Validate(); err != nil {
		return err
	}
	return s.repo.Update(ctx, c)
}

// ImportCSV loads customers exported by the sales team (name,email per row).
func (s *Service) ImportCSV(ctx context.Context, r io.Reader) (int, error) {
	rows, err := csv.NewReader(r).ReadAll()
	if err != nil {
		return 0, err
	}
	imported := 0
	for _, row := range rows {
		c := &Customer{Name: row[0], Email: row[1], Plan: "free"}
		if _, err := s.repo.Insert(ctx, c); err != nil {
			return imported, err
		}
		imported++
	}
	return imported, nil
}
