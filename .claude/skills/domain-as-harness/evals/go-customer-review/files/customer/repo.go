package customer

import (
	"context"
	"database/sql"
	"sync"
)

// Schema:
//
//	CREATE TABLE customers (
//	    id    BIGSERIAL PRIMARY KEY,
//	    name  TEXT NOT NULL,
//	    email TEXT NOT NULL,
//	    plan  TEXT NOT NULL
//	);
type Repo struct {
	db    *sql.DB
	mu    sync.RWMutex
	cache map[int64]*Customer // read-through cache, shared across requests
}

func NewRepo(db *sql.DB) *Repo {
	return &Repo{db: db, cache: map[int64]*Customer{}}
}

func (r *Repo) Insert(ctx context.Context, c *Customer) (int64, error) {
	err := r.db.QueryRowContext(ctx,
		`INSERT INTO customers (name, email, plan) VALUES ($1, $2, $3) RETURNING id`,
		c.Name, c.Email, c.Plan).Scan(&c.ID)
	if err != nil {
		return 0, err
	}
	r.mu.Lock()
	r.cache[c.ID] = c
	r.mu.Unlock()
	return c.ID, nil
}

func (r *Repo) Update(ctx context.Context, c *Customer) error {
	_, err := r.db.ExecContext(ctx,
		`UPDATE customers SET name = $1, email = $2, plan = $3 WHERE id = $4`,
		c.Name, c.Email, c.Plan, c.ID)
	if err != nil {
		return err
	}
	r.mu.Lock()
	r.cache[c.ID] = c
	r.mu.Unlock()
	return nil
}

func (r *Repo) FindByID(ctx context.Context, id int64) (*Customer, error) {
	r.mu.RLock()
	if c, ok := r.cache[id]; ok {
		r.mu.RUnlock()
		return c, nil
	}
	r.mu.RUnlock()
	c := &Customer{}
	err := r.db.QueryRowContext(ctx,
		`SELECT id, name, email, plan FROM customers WHERE id = $1`, id).
		Scan(&c.ID, &c.Name, &c.Email, &c.Plan)
	if err != nil {
		return nil, err
	}
	r.mu.Lock()
	r.cache[id] = c
	r.mu.Unlock()
	return c, nil
}

func (r *Repo) EmailExists(ctx context.Context, email string) (bool, error) {
	var n int
	err := r.db.QueryRowContext(ctx,
		`SELECT COUNT(*) FROM customers WHERE email = $1`, email).Scan(&n)
	return n > 0, err
}
